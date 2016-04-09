/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.prism.show.PagePreviewChanges;
import com.evolveum.midpoint.web.security.SecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAbstractRole;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAdminFocus<F extends FocusType> extends PageAdminObjectDetails<F>
		implements ProgressReportingAwarePage {

	public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";

	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;
	private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;	

	private static final String DOT_CLASS = PageAdminFocus.class.getName() + ".";
	private static final String OPERATION_LOAD_FOCUS = DOT_CLASS + "loadFocus";
	private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
	private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";

	private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

	private static final String ID_SHADOWS = "shadows";
	private static final String ID_ASSIGNMENTS = "assignments";

	private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";

	private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);


	@Override
	protected void initializeModel(final PrismObject<F> objectToEdit) {
		super.initializeModel(objectToEdit);
		
		projectionModel = new LoadableModel<List<FocusSubwrapperDto<ShadowType>>>(false) {

			@Override
			protected List<FocusSubwrapperDto<ShadowType>> load() {
				return loadShadowWrappers();
			}
		};

		assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

			@Override
			protected List<AssignmentEditorDto> load() {
				return loadAssignments();
			}
		};

	}

	public LoadableModel<List<FocusSubwrapperDto<ShadowType>>> getProjectionModel() {
		return projectionModel;
	}

	public LoadableModel<List<AssignmentEditorDto>> getAssignmentsModel() {
		return assignmentsModel;
	}

	public List<FocusSubwrapperDto<ShadowType>> getFocusShadows() {
		return projectionModel.getObject();
	}
	
	public List<AssignmentEditorDto> getFocusAssignments() {
		return assignmentsModel.getObject();
	}

	protected void reviveModels() throws SchemaException {
		super.reviveModels();
		WebComponentUtil.revive(projectionModel, getPrismContext());
		WebComponentUtil.revive(assignmentsModel, getPrismContext());
	}

	protected ObjectWrapper<F> loadFocusWrapper(PrismObject<F> userToEdit) {
		ObjectWrapper<F> objectWrapper = super.loadObjectWrapper(userToEdit);
		return objectWrapper;
	}

	@Override
	public void finishProcessing(AjaxRequestTarget target, OperationResult result) {

		if (previewRequested) {
			finishPreviewProcessing(target, result);
			return;
		}
        if (result.isSuccess()) {
            UserType user = null;
            if (getObjectWrapper().getObject().asObjectable() instanceof UserType){
                user = (UserType) getObjectWrapper().getObject().asObjectable();
            }
            Session.get().setLocale(WebModelServiceUtils.getLocale(user));
        }
		boolean userAdded = getDelta() != null && getDelta().isAdd() && StringUtils.isNotEmpty(getDelta().getOid());
		if (!isKeepDisplayingResults() && getProgressReporter().isAllSuccess()
				&& (userAdded || !result.isFatalError())) { // TODO
			showResult(result);
			// todo refactor this...what is this for? why it's using some
			// "shadow" param from result???
			PrismObject<F> focus = getObjectWrapper().getObject();
			F focusType = focus.asObjectable();
			for (ObjectReferenceType ref : focusType.getLinkRef()) {
				Object o = findParam("shadow", ref.getOid(), result);
				if (o != null && o instanceof ShadowType) {
					ShadowType accountType = (ShadowType) o;
					OperationResultType fetchResult = accountType.getFetchResult();
						showResult(OperationResult.createOperationResult(fetchResult), false);
					
				}
			}
			redirectBack();
		} else {
            getProgressReporter().showBackButton(target);
            getProgressReporter().hideAbortButton(target);
            showResult(result);
			target.add(getFeedbackPanel());

			// if we only stayed on the page because of displaying results, hide
			// the Save button
			// (the content of the page might not be consistent with reality,
			// e.g. concerning the accounts part...
			// this page was not created with the repeated save possibility in
			// mind)
			if (userAdded || !result.isFatalError()) {
//				progressReporter.hideSaveButton(target);
			}
		}
	}

	private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
		showResult(result);
		target.add(getFeedbackPanel());
		setResponsePage(new PagePreviewChanges(getProgressReporter().getPreviewResult(), getModelInteractionService(), this));
		// TODO implement "back" functionality correctly
	}

	private List<FocusSubwrapperDto<ShadowType>> loadShadowWrappers() {
		// Load the projects with noFetch by default. Only load the full projection on-denand.
		// The full projection load happens when loadFullShadow() is explicitly invoked.
		return loadSubwrappers(ShadowType.class, UserType.F_LINK_REF, true);
	}
	
	public void loadFullShadow(FocusSubwrapperDto<ShadowType> shadowWrapperDto) {
		ObjectWrapper<ShadowType> shadowWrapperOld = shadowWrapperDto.getObject();
		Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
		FocusSubwrapperDto<ShadowType> shadowWrapperDtoNew = loadSubWrapperDto(ShadowType.class, shadowWrapperOld.getObject().getOid(), false, task);
		ObjectWrapper<ShadowType> shadowWrapperNew = shadowWrapperDtoNew.getObject();
		shadowWrapperOld.copyRuntimeStateTo(shadowWrapperNew);
		shadowWrapperDto.setObject(shadowWrapperNew);
	}

	@Override
	protected List<FocusSubwrapperDto<OrgType>> loadOrgWrappers() {
		return loadSubwrappers(OrgType.class, UserType.F_PARENT_ORG_REF, false);
	}

	private <S extends ObjectType> List<FocusSubwrapperDto<S>> loadSubwrappers(Class<S> type,
			QName propertyToLoad, boolean noFetch) {
		List<FocusSubwrapperDto<S>> list = new ArrayList<>();

		ObjectWrapper<F> focusWrapper = getObjectModel().getObject();
		PrismObject<F> focus = focusWrapper.getObject();
		PrismReference prismReference = focus.findReference(new ItemPath(propertyToLoad));
		if (prismReference == null) {
			return new ArrayList<>();
		}
		List<PrismReferenceValue> references = prismReference.getValues();

		Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
		for (PrismReferenceValue reference : references) {
			FocusSubwrapperDto<S> subWrapper = loadSubWrapperDto(type, reference.getOid(), noFetch, task);
			if (subWrapper != null) {
				list.add(subWrapper);
			}
		}

		return list;
	}

	private <S extends ObjectType> FocusSubwrapperDto<S> loadSubWrapperDto(Class<S> type, String oid, boolean noFetch, Task task) {
		if (oid == null) {
			return null;
		}
		OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
		String resourceName = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> loadOptions = null;
			if (ShadowType.class.equals(type)) {
				loadOptions = SelectorOptions.createCollection(ShadowType.F_RESOURCE,
						GetOperationOptions.createResolve());
			} 
			
			if (noFetch) {
				GetOperationOptions rootOptions = SelectorOptions.findRootOptions(loadOptions);
				if (rootOptions == null) {
					loadOptions.add(new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch()));
				} else {
					rootOptions.setNoFetch(true);
				}
			}

			PrismObject<S> projection = WebModelServiceUtils.loadObject(type, oid, loadOptions, this,
					task, subResult);
			if (projection == null) {
				// No access, just skip it
				return null;
			}
			S projectionType = projection.asObjectable();

			OperationResultType fetchResult = projectionType.getFetchResult();

			StringBuilder description = new StringBuilder();
			if (ShadowType.class.equals(type)) {
				ShadowType shadowType = (ShadowType) projectionType;
				ResourceType resource = shadowType.getResource();
				resourceName = WebComponentUtil.getName(resource);

				if (shadowType.getIntent() != null) {
					description.append(shadowType.getIntent()).append(", ");
				}
			} else if (OrgType.class.equals(type)) {
				OrgType orgType = (OrgType) projectionType;
				resourceName = orgType.getDisplayName() != null
						? WebComponentUtil.getOrigStringFromPoly(orgType.getDisplayName()) : "";
			}
			description.append(WebComponentUtil.getOrigStringFromPoly(projectionType.getName()));

			ObjectWrapper<S> wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName,
					description.toString(), projection, ContainerStatus.MODIFYING, true, this);
			wrapper.setLoadOptions(loadOptions);
			wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
			wrapper.setSelectable(true);
			wrapper.setMinimalized(true);

			wrapper.initializeContainers(this);

			subResult.computeStatus();
			
			return new FocusSubwrapperDto<S>(wrapper, UserDtoStatus.MODIFY);

		} catch (Exception ex) {
			subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
			LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
			return new FocusSubwrapperDto<S>(false, resourceName, subResult);
		}
	}

	private List<AssignmentEditorDto> loadAssignments() {
		List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

		ObjectWrapper focusWrapper = getObjectModel().getObject();
		PrismObject<F> focus = focusWrapper.getObject();
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {

			list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
		}

		Collections.sort(list);

		return list;
	}

	private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
		OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
		subResult.addParam("targetRef", ref.getOid());
		PrismObject target = null;
		try {
			Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
			Class type = ObjectType.class;
			if (ref.getType() != null) {
				type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
			}
			target = getModelService().getObject(type, ref.getOid(), null, task, subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
			subResult.recordFatalError("Couldn't get assignment target ref.", ex);
		}

		if (!subResult.isHandledError() && !subResult.isSuccess()) {
			showResult(subResult);
		}

		return target;
	}
	
	@Override
	protected void prepareObjectForAdd(PrismObject<F> focus) throws SchemaException {
		super.prepareObjectForAdd(focus);
		F focusType = focus.asObjectable();
		// handle added accounts
		
		List<ShadowType> shadowsToAdd = prepareSubobject(getFocusShadows());
		if (!shadowsToAdd.isEmpty()){
			focusType.getLink().addAll(shadowsToAdd);
		}
		
		
		List<OrgType> orgsToAdd = prepareSubobject(getParentOrgs());
		if (!orgsToAdd.isEmpty()){
			focusType.getParentOrg().addAll(orgsToAdd);
		}

		handleAssignmentForAdd(focus, UserType.F_ASSIGNMENT, assignmentsModel.getObject());
	}
	
	protected void handleAssignmentForAdd(PrismObject<F> focus, QName containerName,
			List<AssignmentEditorDto> assignments) throws SchemaException {
		PrismObjectDefinition userDef = focus.getDefinition();
		PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(containerName);

		// handle added assignments
		// existing user assignments are not relevant -> delete them
		PrismContainer<AssignmentType> assignmentContainer = focus.findOrCreateContainer(containerName);
		if (assignmentContainer != null && !assignmentContainer.isEmpty()){
			assignmentContainer.clear();
		}
		
//		List<AssignmentEditorDto> assignments = getFocusAssignments();
		for (AssignmentEditorDto assDto : assignments) {
			if (UserDtoStatus.DELETE.equals(assDto.getStatus())) {
				continue;
			}

			AssignmentType assignment = new AssignmentType();
			PrismContainerValue value = assDto.getNewValue(getPrismContext());
			assignment.setupContainerValue(value);
			value.applyDefinition(assignmentDef, false);
			assignmentContainer.add(assignment.clone().asPrismContainerValue());

			// todo remove this block [lazyman] after model is updated - it has
			// to remove resource from accountConstruction
			removeResourceFromAccConstruction(assignment);
		}
	}

	@Override
	protected void prepareObjectDeltaForModify(ObjectDelta<F> focusDelta) throws SchemaException {
		super.prepareObjectDeltaForModify(focusDelta);
		// handle accounts
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition<F> objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
		PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
		ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}
		
		refDef = objectDefinition.findReferenceDefinition(FocusType.F_PARENT_ORG_REF);
		refDelta = prepareUserOrgsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}

		// handle assignments
		PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		handleAssignmentDeltas(focusDelta, getFocusAssignments(), def);
	}
	
	
	protected ContainerDelta handleAssignmentDeltas(ObjectDelta<F> focusDelta,
			List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
		ContainerDelta assDelta = new ContainerDelta(new ItemPath(), def.getName(), def, getPrismContext());

		for (AssignmentEditorDto assDto : assignments) {
			PrismContainerValue newValue = assDto.getNewValue(getPrismContext());

			switch (assDto.getStatus()) {
				case ADD:
					newValue.applyDefinition(def, false);
					assDelta.addValueToAdd(newValue.clone());
					break;
				case DELETE:
					PrismContainerValue oldValue = assDto.getOldValue();
					oldValue.applyDefinition(def);
					assDelta.addValueToDelete(oldValue.clone());
					break;
				case MODIFY:
					if (!assDto.isModified(getPrismContext())) {
						LOGGER.trace("Assignment '{}' not modified.", new Object[] { assDto.getName() });
						continue;
					}

					handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
					break;
				default:
					warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
			}
		}

		if (!assDelta.isEmpty()) {
			assDelta = focusDelta.addModification(assDelta);
		}

		// todo remove this block [lazyman] after model is updated - it has to
		// remove resource from accountConstruction
		Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
		for (PrismContainerValue value : values) {
			AssignmentType ass = new AssignmentType();
			ass.setupContainerValue(value);
			removeResourceFromAccConstruction(ass);
		}

		return assDelta;
	}
	
	private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
			PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<F> focusDelta)
					throws SchemaException {
		LOGGER.debug("Handling modified assignment '{}', computing delta.",
				new Object[] { assDto.getName() });

		PrismValue oldValue = assDto.getOldValue();
		Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

		for (ItemDelta delta : deltas) {
			ItemPath deltaPath = delta.getPath().rest();
			ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

			delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
			delta.applyDefinition(deltaDef);

			focusDelta.addModification(delta);
		}
	}
	
	@Override
	protected boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult) {
		if (isForce()) {
			OperationResult result = parentResult.createSubresult("Force delete operation");

			try {
				ObjectDelta<F> forceDeleteDelta = getForceDeleteDelta(userWrapper);
				forceDeleteDelta.revive(getPrismContext());

				if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
					getModelService().executeChanges(WebComponentUtil.createDeltaCollection(forceDeleteDelta),
							options, task, result);
				}
			} catch (Exception ex) {
				result.recordFatalError("Failed to execute delete operation with force.");
				LoggingUtils.logException(LOGGER, "Failed to execute delete operation with force", ex);
				return false;
			}

			result.recomputeStatus();
			result.recordSuccessIfUnknown();
			return true;
		}
		return false;
	}
	
	private ObjectDelta getForceDeleteDelta(ObjectWrapper focusWrapper) throws SchemaException {

		List<FocusSubwrapperDto<ShadowType>> accountDtos = getFocusShadows();
		List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
		ObjectDelta<F> forceDeleteDelta = null;
		for (FocusSubwrapperDto<ShadowType> accDto : accountDtos) {
			if (!accDto.isLoadedOK()) {
				continue;
			}

			if (accDto.getStatus() == UserDtoStatus.DELETE) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject());
				refDeltas.add(refDelta);
			} else if (accDto.getStatus() == UserDtoStatus.UNLINK) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject().getOid());
				refDeltas.add(refDelta);
			}
		}
		if (!refDeltas.isEmpty()) {
			forceDeleteDelta = ObjectDelta.createModifyDelta(focusWrapper.getObject().getOid(), refDeltas,
					getCompileTimeClass(), getPrismContext());
		}
		PrismContainerDefinition def = focusWrapper.getObject().findContainer(UserType.F_ASSIGNMENT)
				.getDefinition();
		if (forceDeleteDelta == null) {
			forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(),
					focusWrapper.getObject().getOid(), getPrismContext());
		}

		handleAssignmentDeltas(forceDeleteDelta, getFocusAssignments(), def);
		return forceDeleteDelta;
	}
	
	private <P extends ObjectType> List<P> prepareSubobject(List<FocusSubwrapperDto<P>> projections) throws SchemaException{
		List<P> projectionsToAdd = new ArrayList<>();
		for (FocusSubwrapperDto<P> projection : projections) {
			if (!projection.isLoadedOK()) {
				continue;
			}

			if (!UserDtoStatus.ADD.equals(projection.getStatus())) {
				warn(getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
				continue;
			}

			ObjectWrapper<P> projectionWrapper = projection.getObject();
			ObjectDelta<P> delta = projectionWrapper.getObjectDelta();
			PrismObject<P> proj = delta.getObjectToAdd();
			WebComponentUtil.encryptCredentials(proj, true, getMidpointApplication());

			projectionsToAdd.add(proj.asObjectable());
		}
		return projectionsToAdd;
	}

	
	@Override
	protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
		return getShadowModifyDeltas(result);
	}

	private List<ObjectDelta<? extends ObjectType>> getShadowModifyDeltas(OperationResult result) {
		List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

		List<FocusSubwrapperDto<ShadowType>> accounts = getFocusShadows();
		OperationResult subResult = null;
		for (FocusSubwrapperDto<ShadowType> account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			try {
				ObjectWrapper accountWrapper = account.getObject();
				ObjectDelta delta = accountWrapper.getObjectDelta();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Account delta computed from {} as:\n{}",
							new Object[] { accountWrapper, delta.debugDump(3) });
				}

				if (!UserDtoStatus.MODIFY.equals(account.getStatus())) {
					continue;
				}

				if (delta.isEmpty()
						&& (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())) {
					continue;
				}

				if (accountWrapper.getOldDelta() != null) {
					delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
				}

				// what is this???
				// subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);

				WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Modifying account:\n{}", new Object[] { delta.debugDump(3) });
				}

				deltas.add(delta);
				// subResult.recordSuccess();
			} catch (Exception ex) {
				// if (subResult != null) {
				result.recordFatalError("Couldn't compute account delta.", ex);
				// }
				LoggingUtils.logException(LOGGER, "Couldn't compute account delta", ex);
			}
		}

		return deltas;
	}
	
	/**
	 * remove this method after model is updated - it has to remove resource
	 * from accountConstruction
	 */
	@Deprecated
	private void removeResourceFromAccConstruction(AssignmentType assignment) {
		ConstructionType accConstruction = assignment.getConstruction();
		if (accConstruction == null || accConstruction.getResource() == null) {
			return;
		}

		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(assignment.getConstruction().getResource().getOid());
		ref.setType(ResourceType.COMPLEX_TYPE);
		assignment.getConstruction().setResourceRef(ref);
		assignment.getConstruction().setResource(null);
	}

	private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

		List<FocusSubwrapperDto<ShadowType>> accounts = getFocusShadows();
		for (FocusSubwrapperDto<ShadowType> accDto : accounts) {
			if (accDto.isLoadedOK()) {
				ObjectWrapper accountWrapper = accDto.getObject();
				accountWrapper.revive(getPrismContext());
				ObjectDelta delta = accountWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				PrismObject<ShadowType> account;
				switch (accDto.getStatus()) {
					case ADD:
						account = delta.getObjectToAdd();
						WebComponentUtil.encryptCredentials(account, true, getMidpointApplication());
						refValue.setObject(account);
						refDelta.addValueToAdd(refValue);
						break;
					case DELETE:
						account = accountWrapper.getObject();
						refValue.setObject(account);
						refDelta.addValueToDelete(refValue);
						break;
					case MODIFY:
						// nothing to do, account modifications were applied
						// before
						continue;
					case UNLINK:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(ShadowType.COMPLEX_TYPE);
						refDelta.addValueToDelete(refValue);
						break;
					default:
						warn(getString("pageAdminFocus.message.illegalAccountState", accDto.getStatus()));
				}
			}
		}

		return refDelta;
	}

	
	private ReferenceDelta prepareUserOrgsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

		List<FocusSubwrapperDto<OrgType>> orgs = getParentOrgs();
		for (FocusSubwrapperDto<OrgType> orgDto : orgs) {
			if (orgDto.isLoadedOK()) {
				ObjectWrapper<OrgType> orgWrapper = orgDto.getObject();
				orgWrapper.revive(getPrismContext());
				ObjectDelta<OrgType> delta = orgWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				switch (orgDto.getStatus()) {
					case ADD:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(OrgType.COMPLEX_TYPE);
						refDelta.addValueToAdd(refValue);
						break;
					case DELETE:
						break;
					case MODIFY:
						break;
					case UNLINK:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(OrgType.COMPLEX_TYPE);
						refDelta.addValueToDelete(refValue);
						break;
					default:
						warn(getString("pageAdminFocus.message.illegalAccountState", orgDto.getStatus()));
				}
			}
		}

		return refDelta;
	}


	protected void prepareFocusDeltaForModify(ObjectDelta<F> focusDelta) throws SchemaException {
		// handle accounts
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition<F> objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
		PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
		ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}
		
		refDef = objectDefinition.findReferenceDefinition(FocusType.F_PARENT_ORG_REF);
		refDelta = prepareUserOrgsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}

		// handle assignments
		PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		handleAssignmentDeltas(focusDelta, getFocusAssignments(), def);
	}

	public void recomputeAssignmentsPerformed(AssignmentPreviewDialog dialog, AjaxRequestTarget target) {
		LOGGER.debug("Recompute user assignments");
		Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
		OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
		ObjectDelta<F> delta;
		Set<AssignmentsPreviewDto> assignmentDtoSet = new TreeSet<>();

		try {
			reviveModels();

			ObjectWrapper<F> userWrapper = getObjectWrapper();
			delta = userWrapper.getObjectDelta();
			if (userWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
			}

			switch (userWrapper.getStatus()) {
				case ADDING:
					PrismObject<F> focus = delta.getObjectToAdd();
					prepareObjectForAdd(focus);
					getPrismContext().adopt(focus, getCompileTimeClass());

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
					} else {
						result.recordSuccess();
					}

					break;
				case MODIFYING:
					prepareFocusDeltaForModify(delta);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
					}

					List<ObjectDelta<? extends ObjectType>> accountDeltas = getShadowModifyDeltas(result);
					Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
						deltas.add(delta);
					}

					for (ObjectDelta accDelta : accountDeltas) {
						if (!accDelta.isEmpty()) {
							accDelta.revive(getPrismContext());
							deltas.add(accDelta);
						}
					}

					break;
				default:
					error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
			}

			ModelContext<UserType> modelContext = null;
			try {
				modelContext = getModelInteractionService()
						.previewChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
			} catch (NoFocusNameSchemaException e) {
				info(getString("pageAdminFocus.message.noUserName"));
				target.add(getFeedbackPanel());
				return;
			}

			DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext
					.getEvaluatedAssignmentTriple();
			Collection<? extends EvaluatedAssignment> evaluatedAssignments = evaluatedAssignmentTriple
					.getNonNegativeValues();

			if (evaluatedAssignments.isEmpty()) {
				info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
				target.add(getFeedbackPanel());
				return;
			}

			List<String> directAssignmentsOids = new ArrayList<>();
			for (EvaluatedAssignment<UserType> evaluatedAssignment : evaluatedAssignments) {
				if (!evaluatedAssignment.isValid()) {
					continue;
				}
				// roles and orgs
				DeltaSetTriple<? extends EvaluatedAbstractRole> evaluatedRolesTriple = evaluatedAssignment
						.getRoles();
				Collection<? extends EvaluatedAbstractRole> evaluatedRoles = evaluatedRolesTriple
						.getNonNegativeValues();
				for (EvaluatedAbstractRole role : evaluatedRoles) {
					if (role.isEvaluateConstructions()) {
						assignmentDtoSet.add(createAssignmentsPreviewDto(role, task, result));
					}
				}

				// all resources
				DeltaSetTriple<EvaluatedConstruction> evaluatedConstructionsTriple = evaluatedAssignment
						.getEvaluatedConstructions(task, result);
				Collection<EvaluatedConstruction> evaluatedConstructions = evaluatedConstructionsTriple
						.getNonNegativeValues();
				for (EvaluatedConstruction construction : evaluatedConstructions) {
					assignmentDtoSet.add(createAssignmentsPreviewDto(construction));
				}
			}

			dialog.updateData(target, new ArrayList<>(assignmentDtoSet), directAssignmentsOids);
			dialog.show(target);

		} catch (Exception e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
			error("Could not create assignments preview. Reason: " + e);
			target.add(getFeedbackPanel());
		}
	}

	private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedAbstractRole evaluatedAbstractRole,
			Task task, OperationResult result) {
		AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
		PrismObject<? extends AbstractRoleType> role = evaluatedAbstractRole.getRole();
		dto.setTargetOid(role.getOid());
		dto.setTargetName(getNameToDisplay(role));
		dto.setTargetDescription(role.asObjectable().getDescription());
		dto.setTargetClass(role.getCompileTimeClass());
		dto.setDirect(evaluatedAbstractRole.isDirectlyAssigned());
		if (evaluatedAbstractRole.getAssignment() != null) {
			if (evaluatedAbstractRole.getAssignment().getTenantRef() != null) {
				dto.setTenantName(nameFromReference(evaluatedAbstractRole.getAssignment().getTenantRef(),
						task, result));
			}
			if (evaluatedAbstractRole.getAssignment().getOrgRef() != null) {
				dto.setOrgRefName(
						nameFromReference(evaluatedAbstractRole.getAssignment().getOrgRef(), task, result));
			}
		}
		return dto;
	}

	private String getNameToDisplay(PrismObject<? extends AbstractRoleType> role) {
		String n = PolyString.getOrig(role.asObjectable().getDisplayName());
		if (StringUtils.isNotBlank(n)) {
			return n;
		}
		return PolyString.getOrig(role.asObjectable().getName());
	}

	private String nameFromReference(ObjectReferenceType reference, Task task, OperationResult result) {
		String oid = reference.getOid();
		QName type = reference.getType();
		Class<? extends ObjectType> clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(type);
		PrismObject<? extends ObjectType> prismObject;
		try {
			prismObject = getModelService().getObject(clazz, oid,
					SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
		} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
				| CommunicationException | ConfigurationException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve name for {}: {}", e,
					clazz.getSimpleName(), oid);
			return "Couldn't retrieve name for " + oid;
		}
		ObjectType object = prismObject.asObjectable();
		if (object instanceof AbstractRoleType) {
			return getNameToDisplay(object.asPrismObject());
		} else {
			return PolyString.getOrig(object.getName());
		}
	}
	
	private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedConstruction evaluatedConstruction) {
		AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
		PrismObject<ResourceType> resource = evaluatedConstruction.getResource();
		dto.setTargetOid(resource.getOid());
		dto.setTargetName(PolyString.getOrig(resource.asObjectable().getName()));
		dto.setTargetDescription(resource.asObjectable().getDescription());
		dto.setTargetClass(resource.getCompileTimeClass());
		dto.setDirect(evaluatedConstruction.isDirectlyAssigned());
		dto.setKind(evaluatedConstruction.getKind());
		dto.setIntent(evaluatedConstruction.getIntent());
		return dto;
	}

	private List<FocusSubwrapperDto<ShadowType>> getSelectedProjections() {
		List<FocusSubwrapperDto<ShadowType>> selected = new ArrayList<>();

		List<FocusSubwrapperDto<ShadowType>> all = projectionModel.getObject();
		for (FocusSubwrapperDto<ShadowType> shadow : all) {
			if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
				selected.add(shadow);
			}
		}

		return selected;
	}

	private List<AssignmentEditorDto> getSelectedAssignments() {
		List<AssignmentEditorDto> selected = new ArrayList<AssignmentEditorDto>();

		List<AssignmentEditorDto> all = assignmentsModel.getObject();
		for (AssignmentEditorDto wrapper : all) {
			if (wrapper.isSelected()) {
				selected.add(wrapper);
			}
		}

		return selected;
	}

	private void addSelectedResourceAssignPerformed(ResourceType resource) {
		AssignmentType assignment = new AssignmentType();
		ConstructionType construction = new ConstructionType();
		assignment.setConstruction(construction);

		try {
			getPrismContext().adopt(assignment, getCompileTimeClass(), new ItemPath(FocusType.F_ASSIGNMENT));
		} catch (SchemaException e) {
			error(getString("Could not create assignment", resource.getName(), e.getMessage()));
			LoggingUtils.logException(LOGGER, "Couldn't create assignment", e);
			return;
		}

		construction.setResource(resource);

		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
		assignments.add(dto);

		dto.setMinimized(false);
		dto.setShowEmpty(true);
	}

	private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables,
			String popupId) {
		ModalWindow window = (ModalWindow) get(popupId);
		window.close(target);

		if (newAssignables.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAssignableSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		for (ObjectType object : newAssignables) {
			try {
				if (object instanceof ResourceType) {
					addSelectedResourceAssignPerformed((ResourceType) object);
					continue;
				}

				AssignmentEditorDtoType aType = AssignmentEditorDtoType.getType(object.getClass());

				ObjectReferenceType targetRef = new ObjectReferenceType();
				targetRef.setOid(object.getOid());
				targetRef.setType(aType.getQname());
				targetRef.setTargetName(object.getName());

				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(targetRef);

				AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
				dto.setMinimized(false);
				dto.setShowEmpty(true);

				assignments.add(dto);
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntAssignObject", object.getName(),
						ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
			}
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_PANEL, ID_ASSIGNMENTS)));
	}

	private void updateShadowActivation(AjaxRequestTarget target, List<FocusSubwrapperDto> accounts,
			boolean enabled) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusSubwrapperDto account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			ObjectWrapper wrapper = account.getObject();
			ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper enabledProperty = (PropertyWrapper) activation
					.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
			if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
				warn(getString("pageAdminFocus.message.noEnabledPropertyFound", wrapper.getDisplayName()));
				continue;
			}
			ValueWrapper value = (ValueWrapper) enabledProperty.getValues().get(0);
			ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
					: ActivationStatusType.DISABLED;
			((PrismPropertyValue) value.getValue()).setValue(status);

			wrapper.setSelected(false);
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_PANEL, ID_SHADOWS)));
	}

	private boolean isAnyAccountSelected(AjaxRequestTarget target) {
		List<FocusSubwrapperDto<ShadowType>> selected = getSelectedProjections();
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void deleteShadowPerformed(AjaxRequestTarget target) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_SHADOW, target);
	}

	private void showModalWindow(String id, AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(id);
		window.show(target);
		target.add(getFeedbackPanel());
	}

	private void deleteAccountConfirmedPerformed(AjaxRequestTarget target,
			List<FocusSubwrapperDto<ShadowType>> selected) {
		List<FocusSubwrapperDto<ShadowType>> accounts = projectionModel.getObject();
		for (FocusSubwrapperDto<ShadowType> account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				accounts.remove(account);
			} else {
				account.setStatus(UserDtoStatus.DELETE);
			}
		}
		target.add(get(createComponentPath(ID_MAIN_PANEL, ID_SHADOWS)));
	}

	private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target,
			List<AssignmentEditorDto> selected) {
		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		for (AssignmentEditorDto assignment : selected) {
			if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
				assignments.remove(assignment);
			} else {
				assignment.setStatus(UserDtoStatus.DELETE);
				assignment.setSelected(false);
			}
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_PANEL, ID_ASSIGNMENTS)));
	}

	private void unlinkShadowPerformed(AjaxRequestTarget target, List<FocusSubwrapperDto<ShadowType>> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusSubwrapperDto account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				continue;
			}
			account.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(get(createComponentPath(ID_MAIN_PANEL, ID_SHADOWS)));
	}

	private void unlockShadowPerformed(AjaxRequestTarget target, List<FocusSubwrapperDto> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusSubwrapperDto account : selected) {
			// TODO: implement unlock
		}
	}

	private void deleteAssignmentPerformed(AjaxRequestTarget target) {
		List<AssignmentEditorDto> selected = getSelectedAssignments();
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAssignmentSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
	}

	private void initConfirmationDialogs() {
		ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_SHADOW,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								getSelectedProjections().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAccountConfirmedPerformed(target, getSelectedProjections());
			}
		};
		add(dialog);

		dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAssignmentConfirm",
								getSelectedAssignments().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAssignmentConfirmedPerformed(target, getSelectedAssignments());
			}
		};
		add(dialog);

		// TODO: uncoment later -> check for unsaved changes
		// dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_CANCEL,
		// createStringResource("pageUser.title.confirmCancel"), new
		// AbstractReadOnlyModel<String>() {
		//
		// @Override
		// public String getObject() {
		// return createStringResource("pageUser.message.cancelConfirm",
		// getSelectedAssignments().size()).getString();
		// }
		// }) {
		//
		// @Override
		// public void yesPerformed(AjaxRequestTarget target) {
		// close(target);
		// setResponsePage(PageUsers.class);
		// // deleteAssignmentConfirmedPerformed(target,
		// getSelectedAssignments());
		// }
		// };
		// add(dialog);
	}
	
	@Override
	protected void performAdditionalValidation(PrismObject<F> object,
			Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {
		
		if (object != null && object.asObjectable() != null) {
			for (AssignmentType assignment : object.asObjectable().getAssignment()) {
				for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
					if (errors == null) {
						errors = validator.validateAssignment(assignment);
					} else {
						errors.addAll(validator.validateAssignment(assignment));
					}
				}
			}
		}
		
	}

}
