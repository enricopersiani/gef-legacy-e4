/*******************************************************************************
 * Copyright 2005, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.mylar.zest.core.internal.graphmodel.nested;

import java.util.Iterator;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.mylar.zest.core.DebugPrint;
import org.eclipse.mylar.zest.core.internal.viewers.Graph;
import org.eclipse.mylar.zest.core.viewers.INestedGraphEntityContentProvider;


/**
 * A factory for creating nodes and relationships for nested graphs.
 * 
 * @author ccallendar
 */
public class NestedGraphModelEntityFactory implements INestedGraphModelFactory {

	private StructuredViewer viewer = null;
	private boolean highlightAdjacentNodes = false;
	
	public NestedGraphModelEntityFactory(StructuredViewer viewer, boolean highlightAdjacentNodes) {
		this.viewer = viewer;
		this.highlightAdjacentNodes = highlightAdjacentNodes;
	}
	
	/**
	 * Creates a new nested graph model
	 * @return NestedGraphModel
	 */
	public NestedGraphModel createModel() {
		return new NestedGraphModel((Graph)viewer.getControl());
	}
	
	private INestedGraphEntityContentProvider getContentProvider() {
		return (INestedGraphEntityContentProvider)viewer.getContentProvider();
	}
	
	private ILabelProvider getLabelProvider() {
		return (ILabelProvider)viewer.getLabelProvider();
	}
	
	
	public NestedGraphModel createModelFromContentProvider( Object inputElement ) {
		NestedGraphModel model = createModel();

		// add the root element
		// TODO this maybe should be optional...
		NestedGraphModelNode rootNode = new NestedGraphModelRootNode(model);
		rootNode.setData(rootNode);
		model.addNode(rootNode.getData(), rootNode);
		rootNode.setSizeInLayout(200, 200);
		
		Object entities[] = getContentProvider().getElements( inputElement );
		// add all root the entities and recursively add their children
		if (entities != null) {
			for ( int i = 0; i < entities.length; i++ ) {
				Object data = entities[i];
				NestedGraphModelNode node = createNode(model, data);
				node.setParent(rootNode);
				rootNode.addChild(node);
				node.setSizeInLayout(100, 100);
				node.setHighlightAdjacentNodes(highlightAdjacentNodes);
				addChildNodes(model, data);
			}
		}
		return model;
	}
	
	/**
	 * Recusively adds the children nodes for the given data object.  The children
	 * are gotten from the content provider.
	 * @param model
	 * @param data
	 */
	private void addChildNodes(NestedGraphModel model, Object data) {
		Object[] childrenData = getContentProvider().getChildren(data);
		if (childrenData != null) {
			for (int j = 0; j < childrenData.length; j++) {
				Object childData = childrenData[j];
				NestedGraphModelNode node = createNode(model, childData);
				node.setSizeInLayout(100, 100);
				node.setHighlightAdjacentNodes(highlightAdjacentNodes);
				
				addChildNodes(model, childData);
			}
		}
	}

	/**
	 * Creates a new NestedGraphModelNode with the given data and adds it to the model.
	 */
	public NestedGraphModelNode createNode(NestedGraphModel model, Object data) {
		NestedGraphModelNode node = getNode(model, data);
		if (node == null) {
			node = new NestedGraphModelNode(model, getLabelProvider().getText( data ), getLabelProvider().getImage(data), data);
			node.setHighlightAdjacentNodes(highlightAdjacentNodes);

			// add the parent
			Object parent = getContentProvider().getParent(data);
			if (parent != null) {
				NestedGraphModelNode parentNode = getNode(model, parent);
				if (parentNode != null) {
					node.setParent(parentNode);
					parentNode.addChild(node);
				}
			}
			
			// add it to the model (must be done after adding the parent!)
			model.addNode(data, node);
			
			// add the sibling, children and parent relationships 
			Object[] related = getContentProvider().getConnectedTo(data);
			if ( related != null ) {
				for ( int i = 0; i < related.length; i++ ) {
					createRelationship(model, null, data, related[i]);
				}
			}
		}
		return node;
	}

	public NestedGraphModelConnection createRelationship(NestedGraphModel model, Object data, Object source, Object dest) {
		NestedGraphModelNode sourceNode = getNode(model, source);
		NestedGraphModelNode destNode = getNode(model, dest);

		if ((sourceNode == null) || (destNode == null)) 
			return null;
		
		// Check if connection already exists
		NestedGraphModelConnection connection;
		for (Iterator iterator =  sourceNode.getTargetConnections().iterator(); iterator.hasNext(); ) {
			//TODO: get connections won't work for directed graphs!
			connection = (NestedGraphModelConnection) iterator.next();
			if ((dest != null) && dest.equals(connection.getSource().getExternalNode())) {
				// We already have a node that goes from source to dest!
				DebugPrint.println("Connection already exists: " + connection);
				return null;
			}
		}
		//DebugPrint.println("Connecting: " + sourceNode + " : " + destNode);
		// Create the connection
		double weight = getContentProvider().getWeight( source, dest );
		connection = new NestedGraphModelConnection(model, data, sourceNode, destNode, false, weight);
		model.addConnection(connection.getExternalConnection(), connection);
		return connection;
	}

	/**
	 * Not supported.
	 */
	public NestedGraphModelConnection createRelationship(NestedGraphModel model, Object data) {
		throw new UnsupportedOperationException("Use createRelationship(model, object, object, object)");
	}
	
	private NestedGraphModelNode getNode(NestedGraphModel model, Object data) {
		NestedGraphModelNode node = (NestedGraphModelNode)model.getInternalNode(data);
		return node;
	}
		
}