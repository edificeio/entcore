/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.aggregation.groups;

import java.util.HashSet;

/**
 * <p>
 * An IndicatorGroup object represents the equivalent of an SQL group by clause and is used by the Indicator object.
 * </p>
 * <p>
 * Multiple groups can be nested, using the addGroup method, and in this case, each combination of groups will be aggregated.
 * </p>
 * For example, for a nested group like <code>new IndicatorGroup("group1").addGroup(new IndicatorGroup("group2").addGroup("group3"))</code>,
 * aggregation will be processed for group1 <b>and</b> group1/group2 <b>and</b> group1/group2/group3.
 *
 */
public class IndicatorGroup {

	//Group key, must match a trace collection type
	private String key;

	//Collection of direct children.
	private HashSet<IndicatorGroup> childGroups = new HashSet<>();
	private IndicatorGroup parent = null;

	//Format
	private boolean isArray = false;

	//Total number of groups & sub groups
	private int totalChildren = 1;

	/**
	 * Creates a new IndicatorGroup.
	 * @param groupKey : The group key, must match a collection type.
	 */
	public IndicatorGroup(String groupKey){
		this.key = groupKey;
	}

	//Propagates
	private void incrementCounter(int add){
		totalChildren += add;
		if(parent != null){
			parent.incrementCounter(add);
		}
	}

	/**
	 * Add a new child group.
	 * @param group : A group key from which the child group will be initialized, must match a collection type.
	 * @return The current group.
	 */
	public IndicatorGroup addChild(String group){
		IndicatorGroup childGroup = new IndicatorGroup(group);
		this.childGroups.add(childGroup);
		incrementCounter(1);
		childGroup.parent = this;
		return this;
	}
	/**
	 * Add a new child group.
	 * @param group : An already initialized group.
	 * @return The current group.
	 */
	public IndicatorGroup addChild(IndicatorGroup group){
		this.childGroups.add(group);
		incrementCounter(group.totalChildren);
		group.parent = this;
		return this;
	}

	/**
	 * Add a new child group.
	 * @param group : A group key from which the child group will be initialized, must match a collection type.
	 * @return The child group.
	 */
	public IndicatorGroup addAndReturnChild(String group){
		IndicatorGroup childGroup = new IndicatorGroup(group);
		this.childGroups.add(childGroup);
		incrementCounter(1);
		childGroup.parent = this;
		return childGroup;
	}
	/**
	 * Add a new child group.
	 * @param group : An already initialized group.
	 * @return The child group.
	 */
	public IndicatorGroup addAndReturnChild(IndicatorGroup group){
		this.childGroups.add(group);
		group.parent = this;
		incrementCounter(group.totalChildren);
		return group;
	}

	/**
	 * Get the group key.
	 * @return Group key
	 */
	public String getKey(){
		return key;
	}

	/**
	 * Get the list of children.
	 * @return The children list.
	 */
	public HashSet<IndicatorGroup> getChildren(){
		return childGroups;
	}

	/**
	 * Get the parent group.
	 * @return Parent group or <code>null</code> if the group has no parent.
	 */
	public IndicatorGroup getParent(){
		return parent;
	}

	/**
	 * Sets the group array format.
	 */
	public IndicatorGroup setArray(boolean mode){
		this.isArray = mode;
		return this;
	}

	/**
	 * True if the group format is an array.
	 */
	public boolean isArray(){
		return this.isArray;
	}

	/**
	 * Gets the total number of children contained in this group and its subgroups.
	 */
	public int getTotalChildren(){
		return this.totalChildren;
	}

	public String toString(){
		if(this.parent != null)
			return this.parent.toString() + "/" + this.key;
		return this.key;
	}

}
