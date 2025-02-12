/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.test.cases;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.utils.tree.TriesTreeLimitExceededException;
import org.slj.mqtt.sn.utils.tree.PathTriesTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TriesTreeTests {

    @Before
    public void setup(){
    }

    @Test
    public void testReadAllFromTree() throws TriesTreeLimitExceededException {
        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        int BRANCHES = 50;
        Set<String> added = new HashSet<>();
        for (int i = 0; i < BRANCHES; i++){
            int depth = ThreadLocalRandom.current().nextInt(1, 10);
            String topic = generateRandomTopic(depth);
            tree.addPath(topic, "foo");
            added.add(topic);
        }
        Set<String> all = tree.getDistinctPaths(true);
        Assert.assertEquals("all distinct topics should be returned by match", added.size(), all.size());
        all.removeAll(added);
        Assert.assertEquals("all topics should exist in both sets", 0, all.size());
    }

    @Test(expected = TriesTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxSegments() throws TriesTreeLimitExceededException {
        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        String topic = generateRandomTopic((int) tree.getMaxPathSegments() + 1);
        tree.addPath(topic, "foo");
    }

    @Test(expected = TriesTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxLength() throws TriesTreeLimitExceededException {
        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        String topic = generateTopicMaxLength((int) tree.getMaxPathSize() + 1);
        tree.addPath(topic, "foo");
    }

    @Test(expected = TriesTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxMembers() throws TriesTreeLimitExceededException {
        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        String topic = generateRandomTopic(10);
        String[] members = new String[(int) tree.getMaxMembersAtLevel() + 1];
        Arrays.fill(members, UUID.randomUUID().toString());
        tree.addPath(topic, members);
    }

    @Test
    public void testTopLevelTokenMatch() throws TriesTreeLimitExceededException {

        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        tree.addPath("/", "foo");
        Assert.assertEquals("first level is a token", 1, tree.searchMembers("/").size());
    }

    @Test
    public void testTopLevelPrefixTokenMatchDistinct() throws TriesTreeLimitExceededException {

        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        tree.addPath("/foo", "foo"); //different things
        tree.addPath("foo", "bar");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 2 distinct branches", 2, tree.getBranchCount());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("/foo").size());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo").size());
    }

    @Test
    public void testTopLevelSuffixTokenMatchDistinct() throws TriesTreeLimitExceededException {

        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        tree.addPath("foo/", "foo"); //different things
        tree.addPath("foo", "bar");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 2 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo/").size());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo").size());
        Assert.assertEquals("top level path sep is distinct from none", 0, tree.searchMembers("/foo").size());
    }

    @Test
    public void testWildcard() throws TriesTreeLimitExceededException {

        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        tree.addPath("foo/bar#", "foo");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 1 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/bar/is/me").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/baris/me").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("moo/bar/is/me").size());
    }

    @Test
    public void testWildpath() throws TriesTreeLimitExceededException {

        PathTriesTree<String> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        tree.addPath("foo/+/is/good", "foo");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 1 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/mar/is/good").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/bar/is/good").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("foo/bar/is/bad").size());
    }

    @Test
    public void testPathExistenceInBigTree() throws TriesTreeLimitExceededException, InterruptedException {

        PathTriesTree<Integer> tree = new PathTriesTree<>(MqttsnConstants.TOPIC_SEPARATOR_REGEX, "/", true);
        String search = "some/member";
        String searchNoMem = "/some/member";

        //-- rememeber the / is a token
        tree.setMaxPathSegments(tree.getMaxPathSegments() * 2 + 1);
        tree.setMaxPathSize(1024 * 4);

        for (int i = 0; i < 200_000; i++){
            String topic = generateRandomTopic(5);
            if(i % 2 == 0){
                tree.addPath(topic,
                        ThreadLocalRandom.current().nextInt(0, 1000));
            } else {
                tree.addPath(topic);
            }
        }

        tree.addPath(search,
                ThreadLocalRandom.current().nextInt(0, 1000));
        tree.addPath(searchNoMem);

//        System.err.println(tree.toTree(System.lineSeparator()));

        Assert.assertTrue("this path should exist", tree.hasPath(search));
        Assert.assertTrue("this path should exist", tree.hasPath(searchNoMem));

        Assert.assertTrue("this path should have members", tree.hasMembers(search));
        Assert.assertFalse("this path should not have members", tree.hasMembers(searchNoMem));

        Assert.assertFalse("this path should not not exist", tree.hasPath("/doesnt/exits"));
        Assert.assertFalse("this path should not not exist nor have members", tree.hasMembers("/doesnt/exits"));

        Assert.assertEquals("path count should match", 200_002, tree.countDistinctPaths(false));
    }

    public static String generateRandomTopic(int segments){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++){
            if(i == 0) sb.append("/");
            int r = ThreadLocalRandom.current().nextInt(0, 1000);
            sb.append(r);
            sb.append("/");
        }
        return sb.toString();
    }

    public static String generateTopicMaxLength(int length){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++){
            sb.append("a");
        }
        return sb.toString();
    }
}
