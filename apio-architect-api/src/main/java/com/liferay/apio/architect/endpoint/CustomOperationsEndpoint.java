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

package com.liferay.apio.architect.endpoint;

import com.liferay.apio.architect.alias.RequestFunction;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.function.throwable.ThrowableFunction;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.apio.architect.operation.Method;
import com.liferay.apio.architect.related.RelatedCollection;
import com.liferay.apio.architect.representor.Representor;
import com.liferay.apio.architect.routes.CollectionRoutes;
import com.liferay.apio.architect.routes.ItemRoutes;
import com.liferay.apio.architect.routes.NestedCollectionRoutes;
import com.liferay.apio.architect.single.model.SingleModel;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.liferay.apio.architect.endpoint.ExceptionSupplierUtil.notAllowed;
import static com.liferay.apio.architect.endpoint.ExceptionSupplierUtil.notFound;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

/**
 * Declares the endpoint for custom operations.
 *
 * @author Javier Gamarra
 */
public class CustomOperationsEndpoint<T, S> {

	public CustomOperationsEndpoint(
		String name, HttpServletRequest httpServletRequest,
		Function<String, Optional<Class<Identifier>>> identifierClassFunction,
		Function<String, Try<SingleModel<T>>> singleModelFunction,
		Supplier<Optional<CollectionRoutes<T>>> collectionRoutesSupplier,
		Supplier<Optional<Representor<T>>> representorSupplier,
		Supplier<Optional<ItemRoutes<T, S>>> itemRoutesSupplier,
		Function<String, Optional<NestedCollectionRoutes<T, Object>>>
			nestedCollectionRoutesFunction,
		Function<com.liferay.apio.architect.uri.Path, S> identifierFunction) {

		_name = name;
		_httpServletRequest = httpServletRequest;
		_identifierClassFunction = identifierClassFunction;
		_singleModelFunction = singleModelFunction;
		_collectionRoutesSupplier = collectionRoutesSupplier;
		_representorSupplier = representorSupplier;
		_itemRoutesSupplier = itemRoutesSupplier;
		_nestedCollectionRoutesFunction = nestedCollectionRoutesFunction;
		_identifierFunction = identifierFunction;
	}

	@DELETE
	@Path("/{operation}/")
	public void addDeleteCollection(
		@PathParam("operation") String operation, Body body) {

		_getSingleModelTry(operation, body, Method.DELETE);
	}

	@DELETE
	@Path("/{id}/{operation}/")
	public void addDeleteItem(
		@PathParam("operation") String operation, @PathParam("id") String id,
		Body body) {

		_getSingleModelTry(operation, id, body, Method.DELETE);
	}

	@GET
	@Path("/{operation}/")
	public Try<SingleModel<T>> addGetCollection(
		@PathParam("operation") String operation, Body body) {

		return _getSingleModelTry(operation, body, Method.GET);
	}

	@GET
	@Path("/{id}/{operation}/")
	public Try<SingleModel<T>> addGetItem(
		@PathParam("operation") String operation, @PathParam("id") String id,
		Body body) {

		return _getSingleModelTry(operation, id, body, Method.GET);
	}

	@Consumes({APPLICATION_JSON, MULTIPART_FORM_DATA})
	@Path("/{operation}/")
	@POST
	public Try<SingleModel<T>> addPostCollection(
		@PathParam("operation") String operation, Body body) {

		return _getSingleModelTry(operation, body, Method.POST);
	}

	@Consumes({APPLICATION_JSON, MULTIPART_FORM_DATA})
	@Path("/{id}/{operation}/")
	@POST
	public Try<SingleModel<T>> addPostItem(
		@PathParam("operation") String operation, @PathParam("id") String id,
		Body body) {

		return _getSingleModelTry(operation, id, body, Method.POST);
	}

	@Consumes({APPLICATION_JSON, MULTIPART_FORM_DATA})
	@Path("/{operation}/")
	@PUT
	public Try<SingleModel<T>> addPutCollection(
		@PathParam("operation") String operation, Body body) {

		return _getSingleModelTry(operation, body, Method.PUT);
	}

	@Consumes({APPLICATION_JSON, MULTIPART_FORM_DATA})
	@Path("/{id}/{operation}/")
	@PUT
	public Try<SingleModel<T>> addPutItem(
		@PathParam("operation") String operation, @PathParam("id") String id,
		Body body) {

		return _getSingleModelTry(operation, id, body, Method.PUT);
	}

	private Try<SingleModel<T>> _getSingleModelTry(String operation, Body body, Method get) {
		return Try.fromOptional(
			_collectionRoutesSupplier::get, notFound(_name)
		).mapOptional(
				collectionRoutes -> collectionRoutes.getCustomRouteFunction()
					.map(
						stringCreateItemFunctionMap ->
							stringCreateItemFunctionMap.get(operation)),
			notAllowed(get, _name)
		).map(
			function -> function.apply(_httpServletRequest)
		).flatMap(
			function -> ((Function<Body, Try<SingleModel<T>>>) function).apply(
				body)
		);
	}

	private Try<SingleModel<T>> _getSingleModelTry(
		String operation, String id, Body body, Method method) {

		return Try.fromOptional(
			_itemRoutesSupplier::get, notAllowed(method, _name)
		).mapOptional(
			ItemRoutes::getCustomRouteFunctions
		).map(
			stringRequestFunctionMap -> stringRequestFunctionMap.get(operation)
		).map(
			functionRequestFunction -> functionRequestFunction.apply(
				_httpServletRequest)
		).map(
			function -> function.apply(_identifierFunction.apply(
				new com.liferay.apio.architect.uri.Path(_name, id)))
		).flatMap(
			function -> function.apply(body)
		);
	}

	private final Supplier<Optional<CollectionRoutes<T>>>
		_collectionRoutesSupplier;
	private final HttpServletRequest _httpServletRequest;
	private final Function<String, Optional<Class<Identifier>>>
		_identifierClassFunction;
	private final Function<com.liferay.apio.architect.uri.Path, S>
		_identifierFunction;
	private final Supplier<Optional<ItemRoutes<T, S>>> _itemRoutesSupplier;
	private final String _name;
	private final Function<String, Optional<NestedCollectionRoutes<T, Object>>>
		_nestedCollectionRoutesFunction;
	private final Supplier<Optional<Representor<T>>> _representorSupplier;
	private final Function<String, Try<SingleModel<T>>> _singleModelFunction;

}