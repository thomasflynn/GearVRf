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

package org.gearvrf;

import org.gearvrf.GVRContext;
import org.gearvrf.SensorEvent.EventGroup;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create an instance of this class to receive {@link SensorEvent}s whenever an
 * input device interacts with a {@link GVRSceneObject}. <p>
 *
 * Note that to successfully receive {@link SensorEvent}s for an object make
 * sure that the sensor is enabled and a valid {@link ISensorEvents} is
 * attached. <p>
 *
 * To attach a {@link ISensorEvents}, set the sensor to the object (e.g., {@link GVRSceneObject}),
 * use {@link GVRSceneObject#getEventReceiver()} to get the {@link GVREventReceiver}, and then
 * use {@link GVREventReceiver#addListener(IEvents)} to add the {@link ISensorEvents}. <p>
 */
public class GVRBaseSensor {
    private static final String TAG = GVRBaseSensor.class.getSimpleName();
    private static final float[] EMPTY_HIT_POINT = new float[3];
    private boolean enabled = true;
    private ArrayList<ControllerData> controllerData;
    protected GVRContext gvrContext;
    protected IEventReceiver owner;
    private DepthComparator depthComparator;
    private boolean depthOrderEnabled;

    /**
     * Constructor for {@link GVRBaseSensor}. By default the depth order property is set to false
     * see {@link GVRBaseSensor#GVRBaseSensor(GVRContext, boolean)}
     * @param gvrContext the {@link GVRContext} associated with the application.
     */
    public GVRBaseSensor(GVRContext gvrContext) {
        this(gvrContext,false);
    }

    /**
     * Constructor for {@link GVRBaseSensor}. {@link GVRBaseSensor} has an option to group
     * {@link SensorEvent} according to depth order. See {@link EventGroup} for more details.
     * Enabling this feature will incur extra cost every time there is a change in the
     * {@link GVRCursorController} position or state. The {@link SensorEvent}s need to be grouped
     * and sorted according to the distance of the associated {@link GVRSceneObject}s from the
     * origin. This feature should only be turned on if needed. The grouping of {@link SensorEvent}s
     * can be useful to apps that have multiple overlapping {@link GVRSceneObject}s and the
     * application has to decide which of the {@link GVRSceneObject}s will handle the
     * {@link SensorEvent}. The depth order sorting can be enabled and disabled using
     * {@link GVRBaseSensor#setDepthOrderEnabled(boolean)}
     * @param gvrContext the {@link GVRContext} associated with the application.
     * @param sendEventsInDepthOrder <code>true</code> if {@link SensorEvent}s are supposed to be
     *                               grouped and sorted according to the depth of the
     *                               {@link GVRSceneObject}. <code>false</code> otherwise.
     */
    public GVRBaseSensor(GVRContext gvrContext, boolean sendEventsInDepthOrder) {
        this.gvrContext = gvrContext;
        controllerData = new ArrayList<GVRBaseSensor.ControllerData>();
        this.depthOrderEnabled = sendEventsInDepthOrder;
        if(sendEventsInDepthOrder) {
            depthComparator = new DepthComparator();
        }
    }

    void setActive(GVRCursorController controller, boolean active) {
        getControllerData(controller).setActive(active);
    }

    void addPickedObject(GVRCursorController controller, GVRPicker.GVRPickedObject pickedObject) {
        ControllerData data = getControllerData(controller);
        Map<GVRSceneObject, GVRPicker.GVRPickedObject> prevHits = data.getPrevHits();
        List<SensorEvent> newHits = data.getNewHits();

        if (prevHits.containsKey(pickedObject.hitObject)) {
            prevHits.remove(pickedObject.hitObject);
        }

        SensorEvent event = SensorEvent.obtain();
        event.setPickedObject(pickedObject);
        event.setOver(true);
        newHits.add(event);
    }

    boolean processList(GVRCursorController controller) {
        final List<SensorEvent> events = new ArrayList<SensorEvent>();

        ControllerData data = getControllerData(controller);
        Map<GVRSceneObject, GVRPicker.GVRPickedObject> prevHits = data.getPrevHits();
        List<SensorEvent> newHits = data.getNewHits();

        // process the previous hit objects to set isOver to false.
        if (prevHits.isEmpty() == false) {
            for (GVRPicker.GVRPickedObject object : prevHits.values()) {
                SensorEvent event = SensorEvent.obtain();
                event.setActive(data.getActive());
                event.setCursorController(controller);
                event.setPickedObject(object);
                event.setOver(false);
                events.add(event);
            }
            if (data.getActive() == false) {
                prevHits.clear();
            }
        }

        if (newHits.isEmpty() == false) {
            for (SensorEvent event : newHits) {
                GVRPicker.GVRPickedObject pickedObject = event.getPickedObject();
                event.setActive(data.getActive());
                event.setCursorController(controller);
                events.add(event);
                prevHits.put(pickedObject.hitObject, pickedObject);
            }
            newHits.clear();
        }
        boolean eventHandled = false;
        GVREventManager eventManager = gvrContext.getEventManager();
        if (events.isEmpty() == false) {
            if(events.size() > 1 && depthOrderEnabled) {
                synchronized (depthComparator) {
                    depthComparator.updateDepthCache(events);
                    Collections.sort(events, depthComparator);
                    depthComparator.clearDepthCache();
                }
            }
            final IEventReceiver ownerCopy = owner;
            for (int i = 0; i < events.size(); i++) {
                SensorEvent event = events.get(i);
                event.setEventGroup(getEventGroup(i,events.size()));
                eventHandled = eventManager.sendEvent(ownerCopy, ISensorEvents.class,
                        "onSensorEvent", event);
                event.recycle();
            }
        }
        return eventHandled;
    }


    private EventGroup getEventGroup(int index, int size) {
        if(depthOrderEnabled) {
            if(index == 0) {
                if(size == 1) {
                    return EventGroup.SINGLE;
                } else {
                    return EventGroup.MULTI_START;
                }
            } else if(index == size-1) {
                return EventGroup.MULTI_STOP;
            } else {
                return EventGroup.MULTI;
            }
        } else {
           return EventGroup.GROUP_DISABLED;
        }
    }

    ControllerData getControllerData(GVRCursorController controller) {
        ControllerData data = controllerData.get(controller.getId());
        if (data == null) {
            data = new ControllerData();
            controllerData.append(controller.getId(), data);
        }
        return data;
    }

    /**
     * Use this method to disable the sensor.
     * 
     * Does not affect the sensor if already disabled.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Use this method to enable the sensor.
     * 
     * Does not affect the sensor if already enabled.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Get the status of the sensor using this call.
     * 
     * @return <code>true</code> if the sensor is enabled. <code>false</code> if
     *         not.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the owner of the sensor. The owner of the sensor can receive
     * events from the sensor.
     *
     * @return The owner object.
     */
    protected IEventReceiver getOwner() {
        return owner;
    }

    /**
     * Sets the owner of the sensor. The owner of the sensor can receive
     * events from the sensor, and must implement the interface {@link IEventReceiver}.
     *
     * @param owner The owner object of the sensor.
     */
    protected void setOwner(IEventReceiver owner) {
        this.owner = owner;
    }


    /**
     * This class keeps track of the all the events generated by a
     * {@link GVRCursorController} on a given {@link GVRBaseSensor}.
     */
    private static class ControllerData {
        private Map<GVRSceneObject, GVRPicker.GVRPickedObject> prevHits;
        private List<SensorEvent> newHits;
        private boolean active;

        public ControllerData() {
            prevHits = new HashMap<GVRSceneObject, GVRPicker.GVRPickedObject>();
            newHits = new ArrayList<SensorEvent>();
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public Map<GVRSceneObject, GVRPicker.GVRPickedObject> getPrevHits() {
            return prevHits;
        }

        public List<SensorEvent> getNewHits() {
            return newHits;
        }

        public boolean getActive() {
            return active;
        }
    }

    /**
     * Checks if the depth ordering is enabled for the {@link GVRBaseSensor}
     * @return <code>true</code> is depth ordering is enabled, <code>false</code> otherwise.
     */
    public boolean isDepthOrderEnabled() {
        return depthOrderEnabled;
    }

    /**
     * Enable or disable the Depth ordering of {@link SensorEvent}s for {@link GVRBaseSensor}.
     * Enabling this feature will incur extra cost every time there is a change in the
     * {@link GVRCursorController} position or state. The {@link SensorEvent}s need to be grouped
     * and sorted according to the distance of the associated {@link GVRSceneObject}s from the
     * origin. This feature should only be turned on if needed. The grouping of {@link SensorEvent}s
     * can be useful to apps that have multiple overlapping {@link GVRSceneObject}s and the
     * application has to decide which of the {@link GVRSceneObject}s will handle the
     * {@link SensorEvent}.
     * @see EventGroup
     * @see GVRBaseSensor#GVRBaseSensor(GVRContext, boolean)
     * @param depthOrderEnabled <code>true</code> to enable depth order, <code>false</code> to
     *                          disable depth order.
     */
    public void setDepthOrderEnabled(boolean depthOrderEnabled) {
        this.depthOrderEnabled = depthOrderEnabled;
        if(depthOrderEnabled && depthComparator == null) {
            depthComparator = new DepthComparator();
        } else if(!depthOrderEnabled && depthComparator != null) {
            depthComparator = null;
        }
    }

    private static class DepthComparator implements Comparator<SensorEvent> {
        private Vector3f hitPoint;
        private Matrix4f modelMatrix;
        private HashMap<SensorEvent, Float> depthCache;

        DepthComparator() {
            hitPoint = new Vector3f(0,0,0);
            modelMatrix = new Matrix4f();
            depthCache = new HashMap<SensorEvent, Float>();
        }

        @Override
        public int compare(SensorEvent lhs, SensorEvent rhs) {

            Float lhsDepth = depthCache.get(lhs);
            Float rhsDepth = depthCache.get(rhs);
            if(lhsDepth < rhsDepth) {
                return -1;
            } else if(lhsDepth > rhsDepth) {
                return 1;
            } else {
                return 0;
            }
        }

        void updateDepthCache(List<SensorEvent> events) {
            for(SensorEvent sensorEvent: events) {
                float[] hitLocation = sensorEvent.getPickedObject().getHitLocation();
                modelMatrix.set(sensorEvent.getPickedObject().getHitObject().getTransform().getModelMatrix());
                GVRPicker.GVRPickedObject pickedObject = sensorEvent.getPickedObject();
                hitPoint.set(hitLocation[0], hitLocation[1], hitLocation[2]);
                hitPoint.mulPosition(modelMatrix);
                float depth = hitPoint.distance(0,0,0);
                depthCache.put(sensorEvent, depth);
            }
        }

        void clearDepthCache() {
            depthCache.clear();
        }
    }
}
