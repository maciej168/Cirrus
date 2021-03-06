package pl.mmajewski.cirrus.gui.storage;

import pl.mmajewski.cirrus.common.model.Host;
import pl.mmajewski.cirrus.common.persistance.HostStorage;
import pl.mmajewski.cirrus.gui.RefreshablePanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Created by Maciej Majewski on 05/12/15.
 */
public class HostStoragePanel implements RefreshablePanel{
    private JPanel hostStoragePanel;
    private JList hostList;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private HostStorage hostStorage = null;

    public HostStoragePanel() {
        hostList.addListSelectionListener(new ListSelectionListener() {
            private Object previous = null;
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Host selectedHost = (Host) hostList.getSelectedValue();
                if(selectedHost!=null) {
                    Host retrieved = hostStorage.fetchHost(selectedHost.getCirrusId());
                    propertyChangeSupport.firePropertyChange("selectedHost", previous, retrieved);
                    previous = retrieved;
                }
            }
        });
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    synchronized public void apply(HostStorage hostStorage){
        this.hostStorage = hostStorage;
        if(hostStorage==null){
            return;
        }

        hostList.setListData(hostStorage.fetchAllHosts().toArray());
    }

    @Override
    public void refresh() {
        if(hostStorage != null){
            apply(hostStorage);
        }
    }
}
