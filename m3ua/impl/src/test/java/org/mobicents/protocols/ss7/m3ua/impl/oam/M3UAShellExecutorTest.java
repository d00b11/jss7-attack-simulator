/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.m3ua.impl.oam;

import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.AssociationListener;
import org.mobicents.protocols.api.AssociationType;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.api.PayloadData;
import org.mobicents.protocols.api.Server;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagement;

/**
 * 
 * @author amit bhayani
 * 
 */
public class M3UAShellExecutorTest {

	M3UAShellExecutor m3uaExec = null;
	private TransportManagement transportManagement = null;
	M3UAManagement clientM3UAMgmt = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUp() throws Exception {
		m3uaExec = new M3UAShellExecutor();

		this.transportManagement = new TransportManagement();

		this.clientM3UAMgmt = new M3UAManagement("M3UAShellExecutorTest");
		this.clientM3UAMgmt.setTransportManagement(this.transportManagement);
		this.clientM3UAMgmt.start();

	}

	@AfterMethod
	public void tearDown() throws Exception {
		// Clean up
		clientM3UAMgmt.getAppServers().clear();
		clientM3UAMgmt.getAspfactories().clear();
		clientM3UAMgmt.getRoute().clear();
		clientM3UAMgmt.stop();

	}

	@Test
	public void testServerCommands() throws Exception {

		m3uaExec.setM3uaManagement(clientM3UAMgmt);

		Association sctpAssociation = this.transportManagement.addAssociation(null, 0, null, 0, "testAssoc1");

		// Test creating new AS testas
		String result = m3uaExec.execute("m3ua as create testas AS mode SE rc 100 traffic-mode loadshare".split(" "));
		assertEquals(result, String.format(M3UAOAMMessages.CREATE_AS_SUCESSFULL, "testas"));

		// Try adding same again
		result = m3uaExec.execute("m3ua as create testas AS mode SE rc 100 traffic-mode loadshare".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_AS_FAIL_NAME_EXIST, "testas"), result);

		// Create AS with only mandatory params
		result = m3uaExec.execute("m3ua as create testas1 AS".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_AS_SUCESSFULL, "testas1"), result);

		// Create AS with all params
		result = m3uaExec.execute("m3ua as create testas2 AS mode DE ipspType CLIENT rc 100 traffic-mode loadshare network-appearance 12".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_AS_SUCESSFULL, "testas2"), result);

		// Create AS of type IPSP
		result = m3uaExec.execute("m3ua as create MTUAS IPSP mode DE ipspType server rc 1 traffic-mode loadshare".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_AS_SUCESSFULL, "MTUAS"), result);

		// create ASP
		result = m3uaExec.execute("m3ua asp create testasp1 testAssoc1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_ASP_SUCESSFULL, "testasp1"), result);

		// Error for same name
		result = m3uaExec.execute("m3ua asp create testasp1 testAssoc1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.CREATE_ASP_FAIL_NAME_EXIST, "testasp1"), result);

		// Error : Try to start Asp without assiging to any As
		result = m3uaExec.execute("m3ua asp start testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ASP_NOT_ASSIGNED_TO_AS, "testasp1"), result);

		// assign ASP to AS
		result = m3uaExec.execute("m3ua as add testas testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ADD_ASP_TO_AS_SUCESSFULL, "testasp1", "testas"), result);

		// add again
		result = m3uaExec.execute("m3ua as add testas testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ADD_ASP_TO_AS_FAIL_ALREADY_ASSIGNED_TO_THIS_AS, "testasp1", "testas"), result);

		// Test Routes
		result = m3uaExec.execute("m3ua route add testas 2 -1 -1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ADD_ROUTE_AS_FOR_DPC_SUCCESSFULL, "testas", 2), result);

		// Start Asp
		result = m3uaExec.execute("m3ua asp start testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ASP_START_SUCESSFULL, "testasp1"), result);
		assertTrue(sctpAssociation.isStarted());

		// manually make Association up
		((TestAssociation) sctpAssociation).signalCommUp();

		// Error : starting Asp again
		result = m3uaExec.execute("m3ua asp start testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ASP_ALREADY_STARTED, "testasp1"), result);

		// Stop Asp
		result = m3uaExec.execute("m3ua asp stop testasp1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.ASP_STOP_SUCESSFULL, "testasp1"), result);

		// Lets wait for 3 seconds so underlying transport is killed
		Thread.sleep(3500);

		assertTrue(!sctpAssociation.isStarted());

		// manually bring down
		((TestAssociation) sctpAssociation).signalCommLost();

		// Remove Asp
		result = m3uaExec.execute("m3ua as remove testas testasp1".split(" "));
		assertEquals(result, String.format(M3UAOAMMessages.REMOVE_ASP_FROM_AS_SUCESSFULL, "testasp1", "testas"));

		// Destroy Asp
		result = m3uaExec.execute("m3ua asp destroy testasp1".split(" "));
		assertEquals(result, String.format(M3UAOAMMessages.DESTROY_ASP_SUCESSFULL, "testasp1"));

		// Error : Destroy As
		result = m3uaExec.execute("m3ua as destroy testas".split(" "));
		assertEquals(result, String.format(M3UAOAMMessages.AS_USED_IN_ROUTE_ERROR, "testas", "2:-1:-1"));
		
		
		// Remove route
		result = m3uaExec.execute("m3ua route remove testas 2 -1 -1".split(" "));
		assertEquals(String.format(M3UAOAMMessages.REMOVE_AS_ROUTE_FOR_DPC_SUCCESSFULL, "testas", 2), result);
		
		// Destroy As
		result = m3uaExec.execute("m3ua as destroy testas".split(" "));
		assertEquals(result, String.format(M3UAOAMMessages.DESTROY_AS_SUCESSFULL, "testas"));

		clientM3UAMgmt.stop();
	}

	class TestAssociation implements Association {

		// Is the Association been started by management?
		private volatile boolean started = false;
		// Is the Association up (connection is established)
		protected volatile boolean up = false;

		private AssociationListener associationListener = null;
		private String name = null;

		TestAssociation(String name) {
			this.name = name;
		}

		@Override
		public AssociationListener getAssociationListener() {
			return this.associationListener;
		}

		@Override
		public String getHostAddress() {
			return null;
		}

		@Override
		public int getHostPort() {
			return 0;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getPeerAddress() {
			return null;
		}

		@Override
		public int getPeerPort() {
			return 0;
		}

		@Override
		public String getServerName() {
			return null;
		}

		@Override
		public boolean isStarted() {
			return this.started;
		}

		@Override
		public void send(PayloadData payloadData) throws Exception {
		}

		@Override
		public void setAssociationListener(AssociationListener associationListener) {
			this.associationListener = associationListener;
		}

		public void signalCommUp() {
			this.up = true;
			this.associationListener.onCommunicationUp(this, 1, 1);
		}

		public void signalCommLost() {
			this.up = false;
			this.associationListener.onCommunicationLost(this);
		}

		@Override
		public IpChannelType getIpChannelType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AssociationType getAssociationType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getExtraHostAddresses() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.mobicents.protocols.api.Association#isConnected()
		 */
		@Override
		public boolean isConnected() {
			return started && up;
		}

		protected void start() {
			this.started = true;
		}

		protected void stop() {
			this.started = false;
		}
	}

	class TransportManagement implements Management {

		private FastMap<String, Association> associations = new FastMap<String, Association>();

		@Override
		public Association addAssociation(String hostAddress, int hostPort, String peerAddress, int peerPort, String assocName) throws Exception {
			TestAssociation testAssociation = new TestAssociation(assocName);
			this.associations.put(assocName, testAssociation);
			return testAssociation;
		}

		@Override
		public Server addServer(String serverName, String hostAddress, int port) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association addServerAssociation(String peerAddress, int peerPort, String serverName, String assocName) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association getAssociation(String assocName) throws Exception {
			return this.associations.get(assocName);
		}

		@Override
		public Map<String, Association> getAssociations() {
			return associations.unmodifiable();
		}

		@Override
		public int getConnectDelay() {
			return 0;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public List<Server> getServers() {
			return null;
		}

		@Override
		public int getWorkerThreads() {
			return 0;
		}

		@Override
		public boolean isSingleThread() {
			return false;
		}

		@Override
		public void removeAssociation(String assocName) throws Exception {

		}

		@Override
		public void removeServer(String serverName) throws Exception {

		}

		@Override
		public void setConnectDelay(int connectDelay) {

		}

		@Override
		public void setSingleThread(boolean arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setWorkerThreads(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void start() throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void startAssociation(String assocName) throws Exception {
			Association association = this.associations.get(assocName);
			if (association != null) {
				((TestAssociation) association).start();
			}
		}

		@Override
		public void startServer(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stop() throws Exception {

		}

		@Override
		public void stopAssociation(String assocName) throws Exception {
			Association association = this.associations.get(assocName);
			if (association != null) {
				((TestAssociation) association).stop();
			}
		}

		@Override
		public void stopServer(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public String getPersistDir() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setPersistDir(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public Association addAssociation(String arg0, int arg1, String arg2, int arg3, String arg4, IpChannelType arg5, String[] extraHostAddresses)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Server addServer(String arg0, String arg1, int arg2, IpChannelType arg3, String[] extraHostAddresses) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association addServerAssociation(String arg0, int arg1, String arg2, String arg3, IpChannelType arg4) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void removeAllResourses() throws Exception {
			// TODO Auto-generated method stub

		}

	}
}
