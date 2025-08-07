/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.chenyilei2016.maintain.client.registry;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @see Registration
 */
public class MergedDelegateRegistration implements Registration {

    ServiceInstance delegate;

    public Map<String, String> metadata;

    public Supplier<Map<String, String>> metadataSupplier;

    public MergedDelegateRegistration(ServiceInstance delegate) {
        this.delegate = delegate;
    }

    public MergedDelegateRegistration(Registration bean, Supplier<Map<String, String>> metadataSupplier) {
        this(bean);
        this.metadataSupplier = metadataSupplier;
    }

    public MergedDelegateRegistration appendMetadata(String key, String val) {
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<>();
        }
        this.metadata.put(key, val);
        return this;
    }

    @Override
    public String getServiceId() {
        return delegate.getServiceId();
    }

    @Override
    public String getHost() {
        return delegate.getHost();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public Map<String, String> getMetadata() {
        if (null == metadata && null == metadataSupplier) {
            return delegate.getMetadata();
        }
        LinkedHashMap<String, String> mergedMetadata = new LinkedHashMap<>();
        if (this.metadata != null) {
            mergedMetadata.putAll(this.metadata);
        }
        if (this.metadataSupplier != null) {
            mergedMetadata.putAll(this.metadataSupplier.get());
        }
        mergedMetadata.putAll(delegate.getMetadata());
        return mergedMetadata;
    }
}
