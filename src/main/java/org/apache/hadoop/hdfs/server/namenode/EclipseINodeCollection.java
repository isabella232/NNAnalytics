/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hadoop.hdfs.server.namenode;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.hadoop.util.GSet;

/**
 * Filters the INode GSet into org.eclipse.collections.ConcurrentHashMap sets. This has trades
 * memory space for look-up speed. Expect slower queries but better memory usage.
 */
public class EclipseINodeCollection implements INodeFilterer {

  @Override
  public Map<INode, INodeWithAdditionalFields> filterFiles(
      GSet<INode, INodeWithAdditionalFields> gset) {
    return StreamSupport.stream(gset.spliterator(), true)
        .filter(INode::isFile)
        .collect(
            Collectors.toConcurrentMap(
                node -> node,
                node -> node,
                throwingMerger(),
                org.eclipse.collections.impl.map.mutable.ConcurrentHashMap::new));
  }

  @Override
  public Map<INode, INodeWithAdditionalFields> filterDirs(
      GSet<INode, INodeWithAdditionalFields> gset) {
    return StreamSupport.stream(gset.spliterator(), true)
        .filter(INode::isDirectory)
        .collect(
            Collectors.toConcurrentMap(
                node -> node,
                node -> node,
                throwingMerger(),
                org.eclipse.collections.impl.map.mutable.ConcurrentHashMap::new));
  }

  private static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }
}
