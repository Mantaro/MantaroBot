/*
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package xyz.avarel.kaiper.runtime.java;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class JavaBeansUtils {
    private static final Map<Class<?>, Map<String, PropertyDescriptor>> cache = new WeakHashMap<>();

    private static synchronized Map<String, PropertyDescriptor> scanAndCacheBeans(Class<?> c) {
        Map<String, PropertyDescriptor> map = new LinkedHashMap<>();
        cache.put(c, map);

        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(c).getPropertyDescriptors()) {
                map.put(descriptor.getName(), descriptor);
            }
        } catch (IntrospectionException e) {
            //ignore
        }

        return map;
    }

    static synchronized Map<String, PropertyDescriptor> getBeanInfo(Class<?> c) {
        if (cache.containsKey(c)) {
            return cache.get(c);
        }

        return scanAndCacheBeans(c);
    }
}
