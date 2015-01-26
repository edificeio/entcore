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

	/**
	 * Creates a new IndicatorGroup.
	 * @param groupKey : The group key, must match a MongoDB trace collection type.
	 */
	public IndicatorGroup(String groupKey){
		this.key = groupKey;
	}

	/**
	 * Add a new child group.
	 * @param group : An already initialized group.
	 * @return The current group.
	 */
	public IndicatorGroup addChild(String group){
		IndicatorGroup childGroup = new IndicatorGroup(group);
		this.childGroups.add(childGroup);
		childGroup.parent = this;
		return this;
	}
	/**
	 * Add a new child group.
	 * @param group : A group key from which the child group will be initialized, must match a MongoDB trace collection type.
	 * @return The current group.
	 */
	public IndicatorGroup addChild(IndicatorGroup group){
		this.childGroups.add(group);
		group.parent = this;
		return this;
	}

	/**
	 * Add a new child group.
	 * @param group : An already initialized group.
	 * @return The child group.
	 */
	public IndicatorGroup addAndReturnChild(String group){
		IndicatorGroup childGroup = new IndicatorGroup(group);
		this.childGroups.add(childGroup);
		childGroup.parent = this;
		return childGroup;
	}
	/**
	 * Add a new child group.
	 * @param group : A group key from which the child group will be initialized, must match a MongoDB trace collection type.
	 * @return The child group.
	 */
	public IndicatorGroup addAndReturnChild(IndicatorGroup group){
		this.childGroups.add(group);
		group.parent = this;
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

}
