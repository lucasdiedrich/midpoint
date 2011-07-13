/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * Connector instance configured for a specific resource.
 * 
 * This is kind of connector facade. It is an API provided by
 * the "Unified Connector Framework" to the midPoint provisioning
 * component. There is no associated SPI yet. That may come in the
 * future when this interface stabilizes a bit.
 * 
 * This interface provides an unified facade to a connector capabilities
 * in the Unified Connector Framework interface. The connector is configured
 * to a specific resource instance and therefore can execute operations on 
 * resource.
 * 
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.
 * 
 * @see ConnectorManager
 *  
 *  TODO: rich operation result
 * 
 * @author Radovan Semancik
 * 
 */
public interface ConnectorInstance {
	
    /**
	 * Retrieves the schema from the resource.
	 * 
	 * The schema may be considered to be an XSD schema, but it is returned in a
	 * "parsed" format and it is in fact a bit stricter and richer midPoint
	 * schema.
	 * 
	 * The schema is always "freshly" fetched from the resource, it is not
	 * cached in any way. Therefore this may be quite an expensive operation.
	 * 
	 * @see Schema
	 * 
	 * @return Up-to-date resource schema.
	 * @throws CommunicationException error in communication to the resource 
	 *				- nothing was fetched.
	 */
	public Schema fetchResourceSchema(OperationResult parentResult) throws CommunicationException, GenericFrameworkException;
	
	/**
	 * Retrieves a specific object from the resource.
	 * 
	 * This method is fetching an object from the resource that is identified
	 * by its primary identifier. It is a "targeted" method in this aspect and
	 * it will fail if the object is not found.
	 * 
	 * The objectClass provided as a parameter to this method must correspond
	 * to one of the object classes in the schema. The object class must match
	 * the object. If it does not, the behavior of this operation is undefined.
	 * 
	 * The returned ResourceObject is "disconnected" from schema. It means that
	 * any call to the getDefinition() method of the returned object will
	 * return null.
	 * 
	 * TODO: object not found error
	 * 
	 * @param objectClass objectClass of the object to fetch (QName).
	 * @param identifiers primary identifiers of the object.
	 * @return object fetched from the resource (no schema)
	 * @throws CommunicationException error in communication to the resource 
	 *				- nothing was fetched.
	 */
	public ResourceObject fetchObject(QName objectClass, Set<ResourceObjectAttribute> identifiers, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException;

	public ResourceObject fetchObject(ResourceObjectDefinition resourceObjectDefinition, Set<ResourceObjectAttribute> identifiers, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException;
	
	/**
	 * Schema aware-version of the fetchObject.
	 * 
	 * TODO
	 * 
	 * @param resourceObjectDefinition
	 * @param identifiers
	 * @return
	 * @throws CommunicationException 
	 */
	//public ResourceObject fetchObject(ResourceObjectDefinition resourceObjectDefinition, Set<ResourceObjectAttribute> identifiers) throws CommunicationException;
	
	// TODO schema-aware version of the operations
	
	/**
	 * Execute iterative search operation.
	 * 
	 * This method will execute search operation on the resource and will pass
	 * any objects that are found. A "handler" callback will be called for each
	 * of the objects found.
	 * 
	 * The call to this method will return only after all the callbacks were
	 * called, therefore it is not asynchronous in a strict sense.
	 * 
	 * 
	 * TODO: filter
	 * 
	 * @param objectClass
	 * @param handler
	 * @throws CommunicationException 
	 */
	public void search(QName objectClass, final ResourceObjectDefinition definition, ResultHandler handler, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;

	/**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what were not determined.
	 * 
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already.
	 * 
	 * The connector may return some (or all) of the attributes of created object. The connector should do
	 * this only such operation is efficient, e.g. in case that the created object is normal return value from
	 * the create operation. The connector must not execute additional operation to fetch the state of
	 * created resource. In case that the new state is not such a normal result, the connector must
	 * return null. Returning empty set means that the connector supports returning of new state, but nothing
	 * was returned (e.g. due to a limiting configuration). Returning null means that connector does not support
	 * returning of new object state and the caller should explicitly invoke fetchObject() in case that the
	 * information is needed.
	 * 
	 * @param object
	 * @param additionalOperations
	 * @throws CommunicationException
	 * @return created object attributes. May be null.
	 */
	public Set<ResourceObjectAttribute> addObject(ResourceObject object, Set<Operation> additionalOperations, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;
	
	/**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what results are we not sure about.
	 * 
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already. 
	 * 
	 * @param identifiers
	 * @param changes
	 * @throws CommunicationException
	 */
	public void modifyObject(QName objectClass, Set<ResourceObjectAttribute> identifiers, Set<Operation> changes, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException;
	
	public void deleteObject(QName objectClass, Set<ResourceObjectAttribute> identifiers, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException;
	
	/**
	 * Creates a live Java object from a token previously serialized to string.
	 * 
	 * Serialized token is not portable to other connectors or other resources.
	 * However, newer versions of the connector should understand tokens generated
	 * by previous connector version.
	 * 
	 * @param serializedToken
	 * @return
	 */
	public Property deserializeToken(Object serializedToken);
	
	/**
	 * Returns the latest token. In other words, returns a token that
	 * corresponds to a current state of the resource. If fetchChanges
	 * is immediately called with this token, nothing should be returned
	 * (Figuratively speaking, neglecting concurrent resource modifications).
	 * 
	 * @return
	 * @throws CommunicationException
	 */
	public Property fetchCurrentToken(QName objectClass, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;
	
	/**
	 * Token may be null. That means "from the beginning of history".
	 * 
	 * @param lastToken
	 * @return
	 */
	public List<Change> fetchChanges(QName objectClass, Property lastToken, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;
	
	//public ValidationResult validateConfiguration(ResourceConfiguration newConfiguration);
	
	//public void applyConfiguration(ResourceConfiguration newConfiguration) throws MisconfigurationException;
	
	// Maybe this should be moved to ConnectorManager? In that way it can also test connector instantiation.
	public void test(OperationResult parentResult);
	
}
