/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 * Objects in a scene.
 ***************************************************************************/
#include <string.h>
#include <math.h>

#include "scene_object.h"

#include "objects/components/camera.h"
#include "objects/components/camera_rig.h"
#include "objects/components/eye_pointee_holder.h"
#include "objects/components/render_data.h"
#include "util/gvr_log.h"
#include "mesh.h"

namespace gvr {
SceneObject::SceneObject() :
        HybridObject(), name_(""), transform_(), render_data_(), camera_(), camera_rig_(), eye_pointee_holder_(), parent_(), children_(), visible_(
                true), in_frustum_(false), query_currently_issued_(false), vis_count_(0), lod_min_range_(0), lod_max_range_(MAXFLOAT), using_lod_(false), bounding_volume_dirty_(true) {

    // XXX
    do_cull_me = true;
    // Occlusion query setup
#if _GVRF_USE_GLES3_
    queries_ = new GLuint[1];
    glGenQueries(1, queries_);
#endif
}

SceneObject::~SceneObject() {
#if _GVRF_USE_GLES3_
    delete queries_;
#endif
}

void SceneObject::attachTransform(SceneObject* self, Transform* transform) {
    if (transform_) {
        detachTransform();
    }
    SceneObject* owner_object(transform->owner_object());
    if (owner_object) {
        owner_object->detachRenderData();
    }
    transform_ = transform;
    transform_->set_owner_object(self);
    dirtyBoundingVolume();
}

void SceneObject::detachTransform() {
    if (transform_) {
        transform_->removeOwnerObject();
        transform_ = NULL;
        dirtyBoundingVolume();
    }
    dirtyBoundingVolume();
}

void SceneObject::attachRenderData(SceneObject* self, RenderData* render_data) {
    if (render_data_) {
        detachRenderData();
    }
    SceneObject* owner_object(render_data->owner_object());
    if (owner_object) {
        owner_object->detachRenderData();
    }
    render_data_ = render_data;
    render_data->set_owner_object(self);
    dirtyBoundingVolume();
}

void SceneObject::detachRenderData() {
    if (render_data_) {
        render_data_->removeOwnerObject();
        render_data_ = NULL;
        dirtyBoundingVolume();
    }
    dirtyBoundingVolume();
}

void SceneObject::attachCamera(SceneObject* self, Camera* camera) {
    if (camera_) {
        detachCamera();
    }
    SceneObject* owner_object(camera->owner_object());
    if (owner_object) {
        owner_object->detachCamera();
    }
    camera_ = camera;
    camera_->set_owner_object(self);
}

void SceneObject::detachCamera() {
    if (camera_) {
        camera_->removeOwnerObject();
        camera_ = NULL;
    }
}

void SceneObject::attachCameraRig(SceneObject* self, CameraRig* camera_rig) {
    if (camera_rig_) {
        detachCameraRig();
    }
    SceneObject* owner_object(camera_rig->owner_object());
    if (owner_object) {
        owner_object->detachCameraRig();
    }
    camera_rig_ = camera_rig;
    camera_rig_->set_owner_object(self);
}

void SceneObject::detachCameraRig() {
    if (camera_rig_) {
        camera_rig_->removeOwnerObject();
        camera_rig_ = NULL;
    }
}

void SceneObject::attachEyePointeeHolder(
        SceneObject* self,
        EyePointeeHolder* eye_pointee_holder) {
    if (eye_pointee_holder_) {
        detachEyePointeeHolder();
    }
    SceneObject* owner_object(eye_pointee_holder->owner_object());
    if (owner_object) {
        owner_object->detachEyePointeeHolder();
    }
    eye_pointee_holder_ = eye_pointee_holder;
    eye_pointee_holder_->set_owner_object(self);
}

void SceneObject::detachEyePointeeHolder() {
    if (eye_pointee_holder_) {
        eye_pointee_holder_->removeOwnerObject();
        eye_pointee_holder_ = NULL;
    }
}

void SceneObject::addChildObject(SceneObject* self, SceneObject* child) {
    for (SceneObject* parent = parent_; parent; parent = parent->parent_) {
        if (child == parent) {
            std::string error =
                    "SceneObject::addChildObject() : cycle of scene objects is not allowed.";
            LOGE("%s", error.c_str());
            throw error;
        }
    }
    children_.push_back(child);
    child->parent_ = self;
    child->transform()->invalidate(false);
    dirtyBoundingVolume();
}

void SceneObject::removeChildObject(SceneObject* child) {
    if (child->parent_ == this) {
        children_.erase(std::remove(children_.begin(), children_.end(), child),
                children_.end());
        child->parent_ = NULL;
        dirtyBoundingVolume();
    }
    dirtyBoundingVolume();
}

int SceneObject::getChildrenCount() const {
    return children_.size();
}

SceneObject* SceneObject::getChildByIndex(int index) {
    if (index < children_.size()) {
        return children_[index];
    } else {
        std::string error = "SceneObject::getChildByIndex() : Out of index.";
        throw error;
    }
}

void SceneObject::set_visible(bool visibility = true) {

    //HACK
    //If checked every frame, queries may return
    //an inconsistent result when used with bounding boxes.

    //We need to make sure that the object's visibility status is consistent before
    //changing the status to avoid flickering artifacts.

    if (visibility == true)
        vis_count_++;
    else
        vis_count_--;

    if (vis_count_ > check_frames_) {
        visible_ = true;
        vis_count_ = 0;
    } else if (vis_count_ < (-1 * check_frames_)) {
        visible_ = false;
        vis_count_ = 0;
    }
}

bool SceneObject::isColliding(SceneObject *scene_object) {

    //Get the transformed bounding boxes in world coordinates and check if they intersect
    //Transformation is done by the getTransformedBoundingBoxInfo method in the Mesh class

    float this_object_bounding_box[6], check_object_bounding_box[6];

    glm::mat4 this_object_model_matrix =
            this->render_data()->owner_object()->transform()->getModelMatrix();
    this->render_data()->mesh()->getTransformedBoundingBoxInfo(
            &this_object_model_matrix, this_object_bounding_box);

    glm::mat4 check_object_model_matrix =
            scene_object->render_data()->owner_object()->transform()->getModelMatrix();
    scene_object->render_data()->mesh()->getTransformedBoundingBoxInfo(
            &check_object_model_matrix, check_object_bounding_box);

    bool result = (this_object_bounding_box[3] > check_object_bounding_box[0]
            && this_object_bounding_box[0] < check_object_bounding_box[3]
            && this_object_bounding_box[4] > check_object_bounding_box[1]
            && this_object_bounding_box[1] < check_object_bounding_box[4]
            && this_object_bounding_box[5] > check_object_bounding_box[2]
            && this_object_bounding_box[2] < check_object_bounding_box[5]);

    return result;
}

void SceneObject::dirtyBoundingVolume() {
    if(bounding_volume_dirty_) {
        return;
    }

    bounding_volume_dirty_ = true;

    if(parent_ != NULL) {
        parent_->dirtyBoundingVolume();
    }
}

BoundingVolume& SceneObject::getBoundingVolume() {
    if(!bounding_volume_dirty_) {
        return transformed_bounding_volume_;
    }

    transformed_bounding_volume_.reset();
    /*
    glm::vec4 center(transformed_bounding_volume_.center(), 1.0f);
    transformed_bounding_volume_.expand(center, 17.425493f);
    */
    if(render_data_ && render_data_->mesh()) {
        bounding_volume_.expand(render_data_->mesh()->getBoundingVolume());
        transformed_bounding_volume_.transform(bounding_volume_, transform()->getModelMatrix());
    }

    glm::vec3 center = bounding_volume_.center();
    float radius = bounding_volume_.radius();
    glm::vec3 min = bounding_volume_.min_corner();
    glm::vec3 max = bounding_volume_.max_corner();
    LOGD("name: %s\n", name_.c_str());
    LOGD("b4 center: %f, %f, %f\n", center[0], center[1], center[2]);
    LOGD("b4 radius: %f\n", radius);
    LOGD("b4 min: %f, %f, %f\n", min[0], min[1], min[2]);
    LOGD("b4 max: %f, %f, %f\n", max[0], max[1], max[2]);

    /* XXX with this code commented out, things work.  with it in, doesn't work.
    for(int i=0; i<children_.size(); i++) {
        SceneObject *child = children_[i];
        // XXX transformed_bounding_volume_.expand(child->getBoundingVolume());
        bounding_volume_.expand(child->getBoundingVolume());
    }
    */

    LOGD("after kids\n");
    center = bounding_volume_.center();
    radius = bounding_volume_.radius();
    LOGD("af center: %f, %f, %f\n", center[0], center[1], center[2]);
    LOGD("af radius: %f\n", radius);
    LOGD("af min: %f, %f, %f\n", min[0], min[1], min[2]);
    LOGD("af max: %f, %f, %f\n", max[0], max[1], max[2]);

    // XXX return transformed_bounding_volume_;
    return bounding_volume_;
}

float planeDistanceToPoint(float plane[4], glm::vec3 &compare_point) {
    glm::vec3 normal = glm::vec3(plane[0], plane[1], plane[2]);
    glm::normalize(normal);
    float distance_to_origin = plane[3];
    float distance = glm::dot(compare_point, normal) + distance_to_origin;

    return distance;
}

bool sphereInFrustum(float frustum[6][4], BoundingVolume &sphere) {
    glm::vec3 center = sphere.center();
    float radius = sphere.radius();

    for(int i=0; i<6; i++) {
        float distance = planeDistanceToPoint(frustum[i], center);
        if(distance < -radius) {
            return false; // outside
        } else if(distance < radius) {
            return true; // intersect
        }
    }

    return true; // fully inside
}

bool SceneObject::cull(Camera *camera, glm::mat4 vp_matrix) {
    if (!visible_) {
        return true;
    }

    // XXX
    if(!do_cull_me) {
        return false;
    }

    /*
    if(render_data_ == NULL || render_data_->mesh() == NULL) {
        return false;
    }
    */

    // is in frustum?
    glm::mat4 mvp_matrix_tmp(vp_matrix * transform_->getModelMatrix());

    // Frustum
    float frustum[6][4];

    // Matrix to array
    float mvp_matrix_array[16] = { 0.0 };
    const float *mat_to_array = (const float*) glm::value_ptr(mvp_matrix_tmp);
    //const float *mat_to_array = (const float*) glm::value_ptr(vp_matrix);
    memcpy(mvp_matrix_array, mat_to_array, sizeof(float) * 16);

    // Build the frustum
    build_frustum(frustum, mvp_matrix_array);

    // Calculate current transformed bounding volume
    BoundingVolume volume = getBoundingVolume();
    //const BoundingVolume& volume = render_data_->mesh()->getBoundingVolume();

    // Check for being inside or outside frustum
    bool is_inside = is_cube_in_frustum(frustum, volume);
    //bool is_inside = sphereInFrustum(frustum, volume);

    // Only push those scene objects that are inside of the frustum
    if (!is_inside) {
        set_in_frustum(false);
        return true;
    }

    // check LOD
    // Transform the bounding sphere
    glm::vec4 sphere_center(bounding_volume_.center(), 1.0f);
    glm::vec4 transformed_sphere_center = mvp_matrix_tmp * sphere_center;

    // Calculate distance from camera
    glm::vec3 camera_position =
            camera->owner_object()->transform()->position();
    glm::vec4 position(camera_position, 1.0f);
    glm::vec4 difference = transformed_sphere_center - position;
    float distance = glm::dot(difference, difference);

    // this distance will be used when sorting transparent objects
    if(render_data_) {
        render_data_->set_camera_distance(distance);
    }

    // Check if this is the correct LOD level
    if (!inLODRange(distance)) {
        // not in range, cull me out
        return true;
    }

    set_in_frustum();

    return false;
}

void SceneObject::build_frustum(float frustum[6][4], float mvp_matrix[16]) {
    float t;

    /* Extract the numbers for the RIGHT plane */
    frustum[0][0] = mvp_matrix[3] - mvp_matrix[0];
    frustum[0][1] = mvp_matrix[7] - mvp_matrix[4];
    frustum[0][2] = mvp_matrix[11] - mvp_matrix[8];
    frustum[0][3] = mvp_matrix[15] - mvp_matrix[12];

    /* Normalize the result */
    t = sqrt(
            frustum[0][0] * frustum[0][0] + frustum[0][1] * frustum[0][1]
                    + frustum[0][2] * frustum[0][2]);
    frustum[0][0] /= t;
    frustum[0][1] /= t;
    frustum[0][2] /= t;
    frustum[0][3] /= t;

    /* Extract the numbers for the LEFT plane */
    frustum[1][0] = mvp_matrix[3] + mvp_matrix[0];
    frustum[1][1] = mvp_matrix[7] + mvp_matrix[4];
    frustum[1][2] = mvp_matrix[11] + mvp_matrix[8];
    frustum[1][3] = mvp_matrix[15] + mvp_matrix[12];

    /* Normalize the result */
    t = sqrt(
            frustum[1][0] * frustum[1][0] + frustum[1][1] * frustum[1][1]
                    + frustum[1][2] * frustum[1][2]);
    frustum[1][0] /= t;
    frustum[1][1] /= t;
    frustum[1][2] /= t;
    frustum[1][3] /= t;

    /* Extract the BOTTOM plane */
    frustum[2][0] = mvp_matrix[3] + mvp_matrix[1];
    frustum[2][1] = mvp_matrix[7] + mvp_matrix[5];
    frustum[2][2] = mvp_matrix[11] + mvp_matrix[9];
    frustum[2][3] = mvp_matrix[15] + mvp_matrix[13];

    /* Normalize the result */
    t = sqrt(
            frustum[2][0] * frustum[2][0] + frustum[2][1] * frustum[2][1]
                    + frustum[2][2] * frustum[2][2]);
    frustum[2][0] /= t;
    frustum[2][1] /= t;
    frustum[2][2] /= t;
    frustum[2][3] /= t;

    /* Extract the TOP plane */
    frustum[3][0] = mvp_matrix[3] - mvp_matrix[1];
    frustum[3][1] = mvp_matrix[7] - mvp_matrix[5];
    frustum[3][2] = mvp_matrix[11] - mvp_matrix[9];
    frustum[3][3] = mvp_matrix[15] - mvp_matrix[13];

    /* Normalize the result */
    t = sqrt(
            frustum[3][0] * frustum[3][0] + frustum[3][1] * frustum[3][1]
                    + frustum[3][2] * frustum[3][2]);
    frustum[3][0] /= t;
    frustum[3][1] /= t;
    frustum[3][2] /= t;
    frustum[3][3] /= t;

    /* Extract the FAR plane */
    frustum[4][0] = mvp_matrix[3] - mvp_matrix[2];
    frustum[4][1] = mvp_matrix[7] - mvp_matrix[6];
    frustum[4][2] = mvp_matrix[11] - mvp_matrix[10];
    frustum[4][3] = mvp_matrix[15] - mvp_matrix[14];

    /* Normalize the result */
    t = sqrt(
            frustum[4][0] * frustum[4][0] + frustum[4][1] * frustum[4][1]
                    + frustum[4][2] * frustum[4][2]);
    frustum[4][0] /= t;
    frustum[4][1] /= t;
    frustum[4][2] /= t;
    frustum[4][3] /= t;

    /* Extract the NEAR plane */
    frustum[5][0] = mvp_matrix[3] + mvp_matrix[2];
    frustum[5][1] = mvp_matrix[7] + mvp_matrix[6];
    frustum[5][2] = mvp_matrix[11] + mvp_matrix[10];
    frustum[5][3] = mvp_matrix[15] + mvp_matrix[14];

    /* Normalize the result */
    t = sqrt(
            frustum[5][0] * frustum[5][0] + frustum[5][1] * frustum[5][1]
                    + frustum[5][2] * frustum[5][2]);
    frustum[5][0] /= t;
    frustum[5][1] /= t;
    frustum[5][2] /= t;
    frustum[5][3] /= t;
}

bool SceneObject::is_cube_in_frustum(float frustum[6][4],
        const BoundingVolume &bounding_volume) {
    int p;
    glm::vec3 min_corner = bounding_volume.min_corner();
    glm::vec3 max_corner = bounding_volume.max_corner();

    float Xmin = min_corner[0];
    float Ymin = min_corner[1];
    float Zmin = min_corner[2];
    float Xmax = max_corner[0];
    float Ymax = max_corner[1];
    float Zmax = max_corner[2];

    for (p = 0; p < 6; p++) {
        if (frustum[p][0] * (Xmin) + frustum[p][1] * (Ymin)
                + frustum[p][2] * (Zmin) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmax) + frustum[p][1] * (Ymin)
                + frustum[p][2] * (Zmin) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmin) + frustum[p][1] * (Ymax)
                + frustum[p][2] * (Zmin) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmax) + frustum[p][1] * (Ymax)
                + frustum[p][2] * (Zmin) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmin) + frustum[p][1] * (Ymin)
                + frustum[p][2] * (Zmax) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmax) + frustum[p][1] * (Ymin)
                + frustum[p][2] * (Zmax) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmin) + frustum[p][1] * (Ymax)
                + frustum[p][2] * (Zmax) + frustum[p][3] > 0)
            continue;
        if (frustum[p][0] * (Xmax) + frustum[p][1] * (Ymax)
                + frustum[p][2] * (Zmax) + frustum[p][3] > 0)
            continue;
        return false;
    }
    return true;
}

}
