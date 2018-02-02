/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.apio.architect.wiring.osgi.internal.manager.router;

import static com.liferay.apio.architect.alias.ProvideFunction.curry;
import static com.liferay.apio.architect.wiring.osgi.internal.manager.TypeArgumentProperties.MODEL_CLASS;
import static com.liferay.apio.architect.wiring.osgi.internal.manager.util.ManagerUtil.getGenericClassFromPropertyOrElse;
import static com.liferay.apio.architect.wiring.osgi.internal.manager.util.ManagerUtil.getTypeParamOrFail;

import com.liferay.apio.architect.error.ApioDeveloperError.MustHavePathIdentifierMapper;
import com.liferay.apio.architect.error.ApioDeveloperError.MustHaveValidGenericType;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.apio.architect.operation.Operation;
import com.liferay.apio.architect.router.ItemRouter;
import com.liferay.apio.architect.routes.ItemRoutes;
import com.liferay.apio.architect.routes.ItemRoutes.Builder;
import com.liferay.apio.architect.wiring.osgi.internal.manager.base.BaseManager;
import com.liferay.apio.architect.wiring.osgi.manager.PathIdentifierMapperManager;
import com.liferay.apio.architect.wiring.osgi.manager.ProviderManager;
import com.liferay.apio.architect.wiring.osgi.manager.representable.IdentifierClassManager;
import com.liferay.apio.architect.wiring.osgi.manager.representable.NameManager;
import com.liferay.apio.architect.wiring.osgi.manager.router.ItemRouterManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Hernández
 */
@Component(immediate = true)
public class ItemRouterManagerImpl
	extends BaseManager<ItemRouter, ItemRoutes> implements ItemRouterManager {

	public ItemRouterManagerImpl() {
		super(ItemRouter.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, S extends Identifier> Optional<ItemRoutes<T>>
		getItemRoutesOptional(String name) {

		Optional<Class<S>> optional =
			_identifierClassManager.getIdentifierClassOptional(name);

		return optional.map(
			Class::getName
		).flatMap(
			this::getServiceOptional
		).map(
			routes -> (ItemRoutes<T>)routes
		);
	}

	@Override
	public List<Operation> getOperations(String name) {
		Optional<ItemRoutes<Object>> optional = getItemRoutesOptional(name);

		return optional.map(
			ItemRoutes::getOperations
		).orElseGet(
			Collections::emptyList
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ItemRoutes map(
		ItemRouter itemRouter, ServiceReference<ItemRouter> serviceReference,
		Class<?> clazz) {

		Class<?> modelClass = getGenericClassFromPropertyOrElse(
			serviceReference, MODEL_CLASS,
			() -> getTypeParamOrFail(itemRouter, ItemRouter.class, 0));

		return _getItemRoutes(itemRouter, modelClass, (Class)clazz);
	}

	private <T, S, U extends Identifier<S>> ItemRoutes<T> _getItemRoutes(
		ItemRouter<T, S, U> itemRouter, Class<T> modelClass,
		Class<U> identifierClass) {

		Optional<String> nameOptional = _nameManager.getNameOptional(
			identifierClass.getName());

		String name = nameOptional.orElseThrow(
			() -> new MustHaveValidGenericType(identifierClass));

		Builder<T, S> builder = new Builder<>(
			modelClass, name, curry(_providerManager::provideOptional),
			path -> {
				Optional<S> optional =
					_pathIdentifierMapperManager.mapToIdentifier(
						identifierClass, path);

				return optional.orElseThrow(
					() -> new MustHavePathIdentifierMapper(identifierClass));
			});

		return itemRouter.itemRoutes(builder);
	}

	@Reference
	private IdentifierClassManager _identifierClassManager;

	@Reference
	private NameManager _nameManager;

	@Reference
	private PathIdentifierMapperManager _pathIdentifierMapperManager;

	@Reference
	private ProviderManager _providerManager;

}