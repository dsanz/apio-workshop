/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package apio.architect.workshop.resource;

import apio.architect.workshop.model.OrganizationDTO;
import apio.architect.workshop.model.RecipeDTO;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.OrganizationService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.recipes.model.Recipe;
import com.liferay.recipes.service.RecipeService;
import com.liferay.recipes.workshop.helper.WorkshopHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author Alejandro Hernández
 * @author Victor Galán
 */
@Component(immediate = true, property = "osgi.jaxrs.resource=true", service = Object.class)
public class OrganizationResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("organizations")
    public List<OrganizationDTO> retrieve(@Context User user) throws PortalException {
        return _organizationService.getUserOrganizations(user.getUserId())
            .stream()
            .map(organization -> new OrganizationDTO(organization, _workshopHelper))
            .collect(Collectors.toList());
    }

    @GET
    @Path("organizations/{id}")
    @Produces(APPLICATION_JSON)
    public OrganizationDTO retrieve(@PathParam("id") long id) throws PortalException {
        Organization organization = _organizationService.getOrganization(id);

        return new OrganizationDTO(organization, _workshopHelper);
    }

    @GET
    @Path("organizations/{id}/recipes")
    @Produces(APPLICATION_JSON)
    public List<RecipeDTO> retrieveRecipes(@PathParam("id") long organizationId, @Context User user) throws PortalException {
        Group organizationGroup = _groupService.getOrganizationGroup(user.getCompanyId(), organizationId);

        return _recipeService.getRecipesByGroupId(organizationGroup.getGroupId(), -1, -1)
            .stream()
            .map(recipe -> new RecipeDTO(recipe, _workshopHelper))
            .collect(Collectors.toList());
    }

    @POST
    @Path("organizations/{id}/recipes")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public RecipeDTO create(@PathParam("id") long organizationId, @Context User user, RecipeDTO recipeDTO) throws PortalException {
        Group organizationGroup = _groupService.getOrganizationGroup(user.getCompanyId(), organizationId);

        FileEntry recipeImage = _workshopHelper.createRecipeImage(
            user.getUserId(), organizationGroup.getGroupId(), recipeDTO.getImageURL());

        Duration cookTime = Duration.parse(recipeDTO.getCookTime());
        int hoursToMake = (int) cookTime.toHours();
        int minutesToMake = (int) cookTime.toMinutes() % 60;

        ServiceContext serviceContext = _workshopHelper.getRecipeServiceContext(
            user.getUserId(), organizationGroup.getGroupId(), recipeDTO.getCategory(), recipeDTO.getRecipeAssetTags());

        Date publishedDate = recipeDTO.getPublishedDate() == null ? new Date() : recipeDTO.getPublishedDate();

        Recipe recipe = _recipeService.addRecipe(
            recipeDTO.getName(), recipeDTO.getRegion(), recipeImage.getFileEntryId(), publishedDate,
            hoursToMake, minutesToMake, recipeDTO.getSteps(), recipeDTO.getIngredients(),
            recipeDTO.getVideoURL(), serviceContext);

        return new RecipeDTO(recipe, _workshopHelper);
    }

    @Reference
    private RecipeService _recipeService;

    @Reference
    private OrganizationService _organizationService;

    @Reference
    private GroupLocalService _groupService;

    @Reference
    private WorkshopHelper _workshopHelper;

}