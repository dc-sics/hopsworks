/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.services;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
public enum Group {

    USER("User"),
    ADMIN("Admin");
    
    private final String group;

    private Group(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

}
