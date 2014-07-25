/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.framework.events.bus.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.listener.EventListener;
import won.bot.framework.events.listener.SubscriptionAware;

import java.util.*;
import java.util.concurrent.Executor;

/**
 *
 */
public class AsyncEventBusImpl implements EventBus
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private HashMap<Class<? extends Event>,List<EventListener>> listenerMap = new HashMap<>();
  private Executor executor;
  private Object monitor = new Object();

  public AsyncEventBusImpl(final Executor executor)
  {
    this.executor = executor;
  }

  @Override
  public <T extends Event> void publish(final T event)
  {
    logger.debug("publishing event {}", event);
    //get the list of listeners registered for the event
    List<EventListener> listeners = getEventListenersForEvent(event);
    if (listeners == null || listeners.size() == 0) {
      logger.debug("no listeners registered for event {}, ignoring", event);
      return;
    }
    //execute asynchronously: call the event listeners one after another
    callEventListeners(listeners, event);
  }


  @Override
  public <T extends Event> void subscribe(Class<T> clazz, final EventListener listener)
  {
    logger.debug("subscribing listener {} for type {}", listener, clazz);
    synchronized (monitor) {
      //we want to synchronize so we don't accidentally add or remove listeners at the same time
      List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));
      newListenerList.add(listener);
      this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
      callOnSubscribeIfApplicable(listener, clazz);
    }
  }


  @Override
  public <T extends Event> void unsubscribe(Class<T> clazz, final EventListener listener)
  {
    logger.debug("unsubscribing listener {} for type {}", listener, clazz);
    synchronized (monitor) {
      //we want to synchronize so we don't accidentally add or remove listeners at the same time
      List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));      newListenerList.remove(listener);
      this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
      callOnUnsubscribeIfApplicable(listener, clazz);
    }
  }

  @Override
  public void unsubscribe(final EventListener listener)
  {
    logger.debug("unsubscribing listener {} from all events", listener);
    synchronized (monitor){
      for (Map.Entry<Class<? extends Event>, List<EventListener>> entry: listenerMap.entrySet()){
        boolean unsubscribed = false; //remember if we had to unsubscribe the listener for the current event type
        List<EventListener> listeners = entry.getValue();
        if (listeners == null ) continue;
        listeners = copyOrCreateList(listeners);
        Iterator<EventListener> it = listeners.iterator();
        while(it.hasNext()){
          EventListener subscribedListener = it.next();
          if (subscribedListener.equals(listener)) {
            it.remove();
            unsubscribed = true;
          }
        }
        entry.setValue(listeners);
        if (unsubscribed){
          //if we had to unssubscribe the listener, we may have to call its onUnsubscribe method
          callOnUnsubscribeIfApplicable(listener, entry.getKey());
        }
      }
    }
  }

  private void callEventListeners(final List<EventListener> listeners, final Event event)
  {
    if (listeners == null || listeners.isEmpty()) return;
    this.executor.execute(new Runnable(){
      @Override
      public void run()
      {
        logger.debug("processing event {} with {} listeners", event, listeners.size());
        for (EventListener listener : listeners) {
          try {
            listener.onEvent(event);
          } catch (Exception e) {
            logger.warn("caught exception during execution of event {} on listener {}", event, listener);
            logger.warn("stacktrace:",e);
          }
        }
      }
    });
  }

  private List<EventListener> getEventListenersForEvent(final Event event)
  {
    //the map is secured against concurrent modification, the list inside is unmodifiable
    return listenerMap.get(event.getClass());
  }


  private List<EventListener> copyOrCreateList(final List<EventListener> listenerList)
  {
    if (listenerList == null) return new ArrayList<EventListener>(1);
    List<EventListener> newListenerList = new ArrayList<EventListener>(listenerList.size()+1);
    newListenerList.addAll(listenerList);
    return newListenerList;
  }

  private <T extends Event> void callOnSubscribeIfApplicable(final EventListener listener, final Class<T> clazz)
  {
    if (listener instanceof  SubscriptionAware){
      ((SubscriptionAware) listener).onSubscribe(clazz);
    }
  }

  private <T extends Event> void callOnUnsubscribeIfApplicable(final EventListener listener, final Class<T> clazz)
  {
    if (listener instanceof  SubscriptionAware){
      ((SubscriptionAware) listener).onUnsubscribe(clazz);
    }
  }


}
