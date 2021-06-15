package ru.sur.msc.gui;

import java.awt.*;
import java.util.*;
import java.util.List;


public class DefaultViewController implements ViewContoller {

    public static final String HOME = "home";

    private static Component currentView = null;
    private static List<Component> views;
    private static Map<Component, String> mapNames;

    private static Container parent;
    private static CardLayout cardLayout;

    public DefaultViewController(Container parent, CardLayout cardLayout) {
        this.parent = parent;
        this.cardLayout = cardLayout;
        views = new ArrayList<>(25);
        mapNames = new HashMap<>(25);
    }

    public CardLayout getCardLayout() {
        return cardLayout;
    }

    public Container getParent() {
        return parent;
    }

    public void addView(Component comp, String name) {
        if (!HOME.equals(name)) {
            views.add(comp);
        }
        mapNames.put(comp, name);
        getParent().add(comp, name);
    }

    public void removeView(Component comp, String name) {
        views.remove(comp);
        mapNames.remove(comp);
        getParent().remove(comp);
    }

    @Override
    public void goHome() {
        currentView = null;
        getCardLayout().show(getParent(), HOME);
    }

    @Override
    public void goToMapByName(String name) {
        currentView = null;
        getCardLayout().show(getParent(), name);
    }

    @Override
    public void nextView() {
        if (views.size() > 0) {
            String name = null;
            if (currentView == null) {
                currentView = views.get(0);
                name = mapNames.get(currentView);
            } else {
                int index = views.indexOf(currentView);
                index++;
                if (index >= views.size()) {
                    index = 0;
                }
                currentView = views.get(index);
                name = mapNames.get(currentView);
            }
            getCardLayout().show(getParent(), name);
        }
    }

    @Override
    public void previousView() {
        if (views.size() > 0) {
            String name = null;
            if (currentView == null) {
                currentView = views.get(views.size() - 1);
                name = mapNames.get(currentView);
            } else {
                int index = views.indexOf(currentView);
                index--;
                if (index < 0) {
                    index = views.size() - 1;
                }
                currentView = views.get(index);
                name = mapNames.get(currentView);
            }
            getCardLayout().show(getParent(), name);
        }
    }

}
