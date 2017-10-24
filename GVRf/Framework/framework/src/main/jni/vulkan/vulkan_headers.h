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

#ifndef FRAMEWORK_VULKAN_HEADERS_H
#define FRAMEWORK_VULKAN_HEADERS_H

#ifdef __ANDROID__
#define VK_USE_PLATFORM_ANDROID_KHR
#endif

#ifdef __linux__
#define VK_USE_PLATFORM_XLIB_KHR
#endif

#include "vulkan_wrapper.h"
#include "vulkanInfoWrapper.h"
#include <vector>
#include "glm/glm.hpp"
#include "vulkanCore.h"
#include "vulkan_uniform_block.h"


#endif //FRAMEWORK_VULKAN_HEADERS_H
