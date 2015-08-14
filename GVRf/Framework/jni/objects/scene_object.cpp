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
        return bounding_volume_;
    }

    if(render_data_ && render_data_->mesh()) {
        bounding_volume_.expand(render_data_->mesh()->getBoundingVolume());
    }

    for(int i=0; i<children_.size(); i++) {
        SceneObject *child = children_[i];
        bounding_volume_.expand(child->getBoundingVolume());
    }

    return bounding_volume_;
}

bool SceneObject::cull(Camera *camera, glm::mat4 vp_matrix) {
    if (!visible_) {
        return true;
    }

    if (render_data == NULL || render_data->pass(0)->material() == 0) {
        return true;
    }

    if (render_data->mesh() == NULL) {
        return true;
    }

    if(render_data->render_mask() == 0) {
        return true;
    }

    // is in frustum?
    glm::mat4 model_matrix_tmp(transform()->getModelMatrix());
    glm::mat4 mvp_matrix_tmp(vp_matrix * model_matrix_tmp);

    // Frustum
    float frustum[6][4];

    // Matrix to array
    float mvp_matrix_array[16] = { 0.0 };
    const float *mat_to_array = (const float*) glm::value_ptr(mvp_matrix_tmp);
    memcpy(mvp_matrix_array, mat_to_array, sizeof(float) * 16);

    // Build the frustum
    build_frustum(frustum, mvp_matrix_array);

    const float* bounding_box_info = render_data->mesh()->getBoundingBoxInfo();
    if (bounding_box_info == NULL) {
        return true;
    }

    // Check for being inside or outside frustum
    bool is_inside = is_cube_in_frustum(frustum, bounding_box_info);

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
    render_data_->set_camera_distance(distance);

    // Check if this is the correct LOD level
    if (!inLODRange(distance)) {
        // not in range, cull me out
        return true;
    }

    set_in_frustum();

    return false;
}


}
