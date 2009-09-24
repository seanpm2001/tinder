/**
 * Copyright (C) 2004-2009 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution, or a commercial license
 * agreement with Jive.
 */

package org.xmpp.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dom4j.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;

/**
 * The {@link AbstractComponent} implementation follows the producer/consumer
 * design pattern. The tests in this class verifies that characteristics of this
 * pattern are recognizable in the AbstractComponent implementation.
 * 
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="http://www.igniterealtime.org/issues/browse/TINDER-16">Tinder
 *      bugtracker: TINDER-18</a>
 */
public class AbstractComponentIsConsumerTest {

	private DummyAbstractComponent debugComp = null;

	/**
	 * Starts a fresh new component before each test.
	 */
	@Before
	public void setUp() {
		debugComp = new SlowRespondingThreadNameComponent();
		debugComp.start();
	}

	/**
	 * Shuts down the component after each test.
	 */
	@After
	public void tearDown() {
		if (debugComp != null) {
			debugComp.shutdown();
		}
	}

	/**
	 * The actual work being done by the component (the consumer) should be done
	 * by a different thread than the thread that feeds the input (the
	 * producer).
	 * 
	 * This test uses an AbstractComponent implementation that reports the
	 * thread name that was used during processing. This name is compared with
	 * the name of the thread that feeds the component the request packet (the
	 * producer) to verify that the producer and consumer threads are indeed
	 * different.
	 */
	@Test
	public void consumesOnDifferentThreadTest() throws Exception {
		// setup
		final String producerThreadName = Thread.currentThread().getName();
		final IQ request = new IQ(Type.get);
		request.setChildElement(
				SlowRespondingThreadNameComponent.ELEMENTNAME_THREADNAME,
				SlowRespondingThreadNameComponent.DEBUG_NAMESPACE);

		// do magic
		debugComp.processPacket(request);
		final IQ response = (IQ) debugComp.getSentPacket();

		// verify
		final Element elem = response.getChildElement();
		final String consumerThreadName = elem.getText();
		assertFalse(consumerThreadName.equals(producerThreadName));
	}

	/**
	 * The producer thread should be released as soon as it delivers work to the
	 * consumer, regardless of how long the consumer takes to process a packet.
	 * 
	 * This test uses an AbstractComponent implementation that takes a
	 * significant time to process a packet. The test verifies that the producer
	 * thread finishes work before the consumer threads finish. This verifies
	 * that the workload has been properly offloaded by the producer thread.
	 */
	@Test
	public void consumesAsynchronouslyTest() throws Exception {
		// setup
		final IQ request = new IQ(Type.get);
		request.setChildElement(
				SlowRespondingThreadNameComponent.ELEMENTNAME_SLOWRESPONSE,
				SlowRespondingThreadNameComponent.DEBUG_NAMESPACE);

		// do magic
		final long start = System.currentTimeMillis();
		debugComp.processPacket(request);
		final long end = System.currentTimeMillis();

		// verify
		final long elapsed = end - start;
		assertTrue(elapsed < 2000);

	}
}