/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * @author lazyman
 */
public class LinkIconPanel extends Panel {

    public LinkIconPanel(String id, IModel<ResourceReference> model) {
        this(id, model, null);
    }

    public LinkIconPanel(String id, IModel<ResourceReference> model, IModel<String> titleModel) {
        super(id);

        initLayout(model, titleModel);
    }

    private void initLayout(IModel<ResourceReference> model, IModel<String> titleModel) {
        AjaxLink link = new AjaxLink("link") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target);
            }
        };
        Image image = new Image("image", model);
        if (titleModel != null) {
            image.add(new AttributeModifier("title", titleModel));
        }
        link.add(image);
        link.setOutputMarkupId(true);
        add(link);
    }

    protected AjaxLink getLink() {
        return (AjaxLink) get("link");
    }

    protected void onClickPerformed(AjaxRequestTarget target) {

    }
}
