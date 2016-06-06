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

package org.gearvrf.animation;

/** Optional on-start callback */
public interface GVROnStart {

    /**
     * Optional callback: called when the animation is started.
     * 
     * <p>
     * With the default, {@linkplain GVRRepeatMode#ONCE run-once} animation,
     * this is pretty straight forward: {@code started()} will be called 
     * and the animation will run to completion.
     * Note that repeat mode {@code ONCE}
     * overrides any {@linkplain GVRAnimation#setRepeatCount(int) repeat count.}
     * 
     * <p>
     * The repetitive types {@link GVRRepeatMode#REPEATED REPEATED} and
     * {@link GVRRepeatMode#PINGPONG PINGPONG} <em>do</em> pay attention to the
     * repeat count. With a positive repeat count, {@code started()} will be called and then the animation will run for
     * the specified number of times. Note
     * that the {@link GVRAnimation#DEFAULT_REPEAT_COUNT} is 2.
     * 
     * <p>
     * If the repeat count is negative, the animation will run until you stop
     * it. There are three ways you can do this:
     * 
     * <ul>
     * <li>You can call {@link GVRAnimationEngine#stop(GVRAnimation)}. This will
     * stop the animation immediately, regardless of the current state of the
     * animated property, so this is generally appropriate only for pop-up
     * objects like Please Wait spinners that are about to disappear. 
     *
     * <li>You can call {@link GVRAnimation#setRepeatCount(int)
     * setRepeatCount(0)} to schedule a graceful termination.
     *
     * </ul>
     * 
     * @param animation
     *            The animation that just started, so you can use the same
     *            callback with multiple animations.
     */
    void started(GVRAnimation animation);

}
