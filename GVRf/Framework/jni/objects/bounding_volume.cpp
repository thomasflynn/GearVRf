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
 * The bounding_volume for rendering.
 ***************************************************************************/

#include "bounding_volume.h"
#include "util/gvr_log.h"

namespace gvr {

BoundingVolume::BoundingVolume() {
    reset();
}

void BoundingVolume::reset() {
    center_ = glm::vec3(0.0f, 0.0f, 0.0f);
    radius_ = 0.0f;
    dirty = true;
    min_corner_ = glm::vec3(
           std::numeric_limits<float>::infinity(), 
           std::numeric_limits<float>::infinity(), 
           std::numeric_limits<float>::infinity());
    max_corner_ = glm::vec3(
          -std::numeric_limits<float>::infinity(), 
          -std::numeric_limits<float>::infinity(), 
          -std::numeric_limits<float>::infinity());
}

/* 
 * expand the current volume by the given point
 */
void BoundingVolume::expand(const glm::vec3 point) {
    if(min_corner_[0] > point[0]) {
        min_corner_[0] = point[0];
    }
    if(min_corner_[1] > point[1]) {
        min_corner_[1] = point[1];
    }
    if(min_corner_[2] > point[2]) {
        min_corner_[2] = point[2];
    }

    if(max_corner_[0] < point[0]) {
        max_corner_[0] = point[0];
    }
    if(max_corner_[1] < point[1]) {
        max_corner_[1] = point[1];
    }
    if(max_corner_[2] < point[2]) {
        max_corner_[2] = point[2];
    }

    center_ = (min_corner_ + max_corner_)*0.5f;
    radius_ = glm::length(max_corner_ - center_);
}

/* 
 * expand the volume by the incoming center and radius
 */
void BoundingVolume::expand(const glm::vec4 &in_center4, float in_radius) {
    glm::vec3 in_center(in_center4.x, in_center4.y, in_center4.z);
    glm::vec3 center_distance = in_center - center_;
    float length = glm::length(center_distance);

    // if the center is the same and incoming radius is
    // bigger, use that.
    if(length == 0 && in_radius > radius_) {
        radius_ = in_radius;
    } else if((length + in_radius) > radius_) {
        // find the new center by taking the half-way point
        // between the two outer ends of the two spheres.
        // the radius is the distance between the two points
        // divided by two.
        // start by normalizing the center_distance
        float distance = sqrt((center_distance[0]*center_distance[0])+
                (center_distance[1]*center_distance[1])+
                (center_distance[2]*center_distance[2]));
        center_distance[0] /= distance;
        center_distance[1] /= distance;
        center_distance[2] /= distance;
        glm::vec3 outer_point_of_incoming_sphere = in_center + (in_radius*center_distance);
        glm::vec3 outer_point_of_current_sphere = center_ - (radius_*center_distance);
        center_ = (outer_point_of_current_sphere + outer_point_of_incoming_sphere) * 0.5f;
        radius_ = glm::length(outer_point_of_incoming_sphere - outer_point_of_current_sphere) * 0.5f;
    }

    // define the bounding box inside the sphere
    //       .. .. .. 
    //     . -------/ .
    //    . |     r/ | .
    //    . |    /___| .
    //    . |      s | .
    //     .|________|.
    //       .. .. ..
    //
    // for a sphere:
    //             r = sqrt(s^2 + s^2 + s^2)
    //           r^2 = s^2 + s^2 + s^2
    //           r^2 = (s^2)*3
    // sqrt((r^2)/3) = s
    //
    // r is radius_
    // s is side of the triangle
    //
    float side = (float) sqrt(((radius_*radius_)/3.0f));
    min_corner_ = glm::vec3(center_[0] - side,
                            center_[1] - side,
                            center_[2] - side);

    max_corner_ = glm::vec3(center_[0] + side,
                            center_[1] + side,
                            center_[2] + side);
}

/*
 * expand the volume by the incoming volume
*/
void BoundingVolume::expand(const BoundingVolume &volume) {
    const glm::vec4 in_center(volume.center(), 1.0f);
    float in_radius = volume.radius();

    expand(in_center, in_radius);
}

/*
 * make this volume the incoming volume transformed by the matrix
*/
void BoundingVolume::transform(const BoundingVolume &in_volume, glm::mat4 matrix) {
    glm::vec4 center(in_volume.center(), 1.0f);

    // calculate new center
    glm::vec4 transformed_center = center * matrix;

    // calculate new radius
    float radius = in_volume.radius();

    // find the maximum extends of the bounding sphere
    glm::vec4 max_radius_x(transformed_center.x+radius, transformed_center.y, transformed_center.z, 1.0f);
    glm::vec4 max_radius_y(transformed_center.x, transformed_center.y+radius, transformed_center.z, 1.0f);
    glm::vec4 max_radius_z(transformed_center.x, transformed_center.y, transformed_center.z+radius, 1.0f);

    // transform by the matrix
    glm::vec4 transformed_radius_x = max_radius_x * matrix;
    glm::vec4 transformed_radius_y = max_radius_y * matrix;
    glm::vec4 transformed_radius_z = max_radius_z * matrix;

    // calculate distance from the center
    float rx = glm::length(transformed_center - transformed_radius_x);
    float ry = glm::length(transformed_center - transformed_radius_y);
    float rz = glm::length(transformed_center - transformed_radius_z);

    // the new radius is the largest distance from the center
    radius = fmaxf(rx, ry);
    radius = fmaxf(radius, rz);

    // calculate new bounding sphere and bounding box
    expand(transformed_center, radius);
}

//Transform existing axis aligned bounding volume by matrix.
//Implementation of Arvo, James, Transforming Axis-Aligned Bounding Boxes, Graphics Gems
// A - the untransformed box (originalBox)
// B - the transformed box
// M - the rotation + scale
// T - the translation (matrix.Offset?)
//
// for i = 1 ... 3
//     Bmin_i = Bmax_i = T_i
//         for j = 1 ... 3
//             a = M_ij * Amin_j
//             b = M_ij * Amax_j
//             Bmin_i += min(a, b)
//             Bmax_i += max(a, b)
//


void BoundingVolume::transform(const glm::mat4 &matrix) {

    glm::vec3 min = glm::vec3(matrix[3][0], matrix[3][1], matrix[3][2]);
    glm::vec3 max = glm::vec3(matrix[3][0], matrix[3][1], matrix[3][2]);
    for (int i = 0; i < 3; i++)
    {
        for (int j = 0; j < 3; j++)
        {
            float val1 = matrix[i][j] * min_corner_[j];
            float val2 = matrix[i][j] * max_corner_[j];
            if (val1 < val2)
            {
                min[i] += val1;
                max[i] += val2;
            }
            else
            {
                min[i] += val2;
                max[i] += val1;
            }
        }
    }
    min_corner_ = min;
    max_corner_ = max;
    center_ = (min_corner_ + max_corner_)*0.5f;
    radius_ = glm::length(max_corner_ - center_);
}


} // namespace

