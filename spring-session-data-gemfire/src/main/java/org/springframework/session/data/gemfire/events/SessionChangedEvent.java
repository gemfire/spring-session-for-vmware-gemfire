/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.session.Session;

/**
 * {@link SessionChangedEvent} is a Spring {@link ApplicationEvent} fire when the {@link Session} state changes.
 *
 * @author John Blum
 * @see ApplicationEvent
 * @see Session
 * @since 2.2.0
 */
public class SessionChangedEvent extends ApplicationEvent {

	private final Session session;

	/**
	 * Constructs a new instance of {@link SessionChangedEvent} initialized with the given {@link Object source}
	 * and {@link Session}.
	 *
	 * @param source {@link Object} referencing the source of the event.
	 * @param session {@link Session} that changed.
	 * @see Session
	 */
	public SessionChangedEvent(Object source, Session session) {

		super(source);

		this.session = session;
	}

	/**
	 * Gets the {@link Session} that was changed.
	 *
	 * @param <S> {@link Class type} of {@link Session}.
	 * @return a reference to the {@link Session} that is the subject of the change event.
	 * @see Session
	 */
	@SuppressWarnings("unchecked")
	public <S extends Session> S getSession() {
		return (S) this.session;
	}
}
