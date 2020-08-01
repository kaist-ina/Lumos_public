package edu.cmu.hcii.sugilite;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.automation.*;
import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.tracking.SugiliteTrackingHandler;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;

public class SugiliteAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;
    private Automator automator;
    private SugiliteData sugiliteData;
    private StatusIconManager statusIconManager;
    private SugiliteScreenshotManager screenshotManager;
    private Set<Integer> accessibilityEventSetToHandle, accessibilityEventSetToSend, accessibilityEventSetToTrack;
    private Thread automatorThread;
    private Context context;
    private SugiliteTrackingHandler sugilteTrackingHandler;



    public SugiliteAccessibilityService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        statusIconManager = new StatusIconManager(this, sugiliteData, sharedPreferences);
        screenshotManager = new SugiliteScreenshotManager(sharedPreferences, getApplicationContext());
        automator = new Automator(sugiliteData, getApplicationContext(), statusIconManager);
        sugilteTrackingHandler = new SugiliteTrackingHandler(sugiliteData, getApplicationContext());
        availableAlternatives = new HashSet<>();
        context = this;
        try {
            //TODO: periodically check the status of communication controller
            sugiliteData.communicationController = new SugiliteCommunicationController(getApplicationContext(), sugiliteData, sharedPreferences);
            sugiliteData.communicationController.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Integer[] accessibilityEventArrayToHandle = {AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED};
        Integer[] accessiblityEventArrayToSend = {AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED};
        Integer[] accessibilityEventArrayToTrack = {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        };
        accessibilityEventSetToHandle = new HashSet<>(Arrays.asList(accessibilityEventArrayToHandle));
        accessibilityEventSetToSend = new HashSet<>(Arrays.asList(accessiblityEventArrayToSend));
        accessibilityEventSetToTrack = new HashSet<>(Arrays.asList(accessibilityEventArrayToTrack));

        //end recording

        //set default value for the settings
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.putBoolean("root_enabled", true);
        prefEditor.putBoolean("auto_fill_enabled", true);
        prefEditor.commit();
        sugiliteData.clearInstructionQueue();
        if(sugiliteData.errorHandler == null){
            sugiliteData.errorHandler = new ErrorHandler(this);
        }
        if(sugiliteData.trackingName.contentEquals("default")){
            sugiliteData.initiateTracking(sugilteTrackingHandler.getDefaultTrackingName());
        }

        try {
            Toast.makeText(this, "Sugilite Accessibility Service Started", Toast.LENGTH_SHORT).show();
            statusIconManager.addStatusIcon();
        }
        catch (Exception e){
            e.printStackTrace();
            //do nothing
        }


    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

    }

    private HashSet<Map.Entry<String, String>> availableAlternatives;
    Set<String> exceptedPackages = new HashSet<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO problem: the status of "right after click" (try getParent()?)
        //TODO new rootNode method
        final AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        AccessibilityNodeInfo sourceNode = event.getSource();

        //Type of accessibility events to handle in this function
        //return if the event is not among the accessibilityEventArrayToHandle
        if(!accessibilityEventSetToHandle.contains(Integer.valueOf(event.getEventType()))) {
            return;
        }

        //check communication status
        if(sugiliteData.communicationController != null){
            if(!sugiliteData.communicationController.checkConnectionStatus())
                sugiliteData.communicationController.start();
        }

        exceptedPackages.add("edu.cmu.hcii.sugilite");
        exceptedPackages.add("com.android.systemui");
        if (sugiliteData.getInstructionQueueSize() > 0 && !exceptedPackages.contains(event.getPackageName()) && sugiliteData.errorHandler != null){
            //script running in progress
            //invoke the error handler

            //sugiliteData.errorHandler.checkError(event, sugiliteData.peekInstructionQueue(), Calendar.getInstance().getTimeInMillis());
        }

        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //recording in progress
            //skip internal interactions and interactions on system ui
            availableAlternatives.addAll(getAlternativeLabels(sourceNode, rootNode));
            if (accessibilityEventSetToSend.contains(event.getEventType()) && (!exceptedPackages.contains(event.getPackageName()))) {
                File screenshot = null;
                if(sharedPreferences.getBoolean("root_enabled", false)) {
                    //take screenshot
                    try {
                        /*
                        System.out.println("taking screen shot");
                        screenshot = screenshotManager.take(false);
                        */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //start the popup activity
                //startActivity(generatePopUpActivityIntentFromEvent(event, rootNode, screenshot, availableAlternatives));
                /*
                * Jeongmin Kim
                * We need to remove this class
                * */
                RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, getApplicationContext(), generateFeaturePack(event, rootNode, screenshot), sharedPreferences, LayoutInflater.from(getApplicationContext()), RecordingPopUpDialog.TRIGGERED_BY_NEW_EVENT, availableAlternatives);
                recordingPopUpDialog.show();
                availableAlternatives.clear();

            }
        }

        if (sharedPreferences.getBoolean("tracking_in_process", false)) {
            if (accessibilityEventSetToTrack.contains(event.getEventType())) {
                sugilteTrackingHandler.handle(event, sourceNode, generateFeaturePack(event, rootNode, null));
            }
            //background tracking in progress
        }
        SugiliteBlock currentBlock = sugiliteData.peekInstructionQueue();

        if(currentBlock instanceof SugiliteOperationBlock) {
            statusIconManager.refreshStatusIcon(rootNode, ((SugiliteOperationBlock) currentBlock).getElementMatchingFilter());
        }
        else{
            statusIconManager.refreshStatusIcon(null, null);
        }

        boolean retVal = false;


        if(sugiliteData.getInstructionQueueSize() > 0) {
            if(automatorThread == null) {
                // JM - it might be the start point of replay feature.
                // Thus we need to send a request to our proxy for dynamic packet learning.
                automatorThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        automator.handleLiveEvent(rootNode, getApplicationContext());
                        automatorThread = null;
                    }
                });
                automatorThread.start();
            }
        }
    }



    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Toast.makeText(this, "Sugilite Accessibility Service Stopped", Toast.LENGTH_SHORT).show();
        if(statusIconManager != null)
            try {
                statusIconManager.removeStatusIcon();
            }
            catch (Exception e){
                //failed to remove status icon
                e.printStackTrace();
            }
        //windowManager.removeView(statusIcon);

        try {
            sugiliteData.communicationController.unregister();
            sugiliteData.communicationController.stop();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }




    private SugiliteAvailableFeaturePack generateFeaturePack(AccessibilityEvent event, AccessibilityNodeInfo rootNode, File screenshot){
        SugiliteAvailableFeaturePack featurePack = new SugiliteAvailableFeaturePack();
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        AccessibilityNodeInfo parentNode = null;
        if(sourceNode != null) {
            sourceNode.getBoundsInParent(boundsInParents);
            sourceNode.getBoundsInScreen(boundsInScreen);
            parentNode = sourceNode.getParent();
        }
        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = new ArrayList<>(Automator.preOrderTraverse(sourceNode));
        ArrayList<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        if(rootNode != null)
            allNodes = new ArrayList<>(Automator.preOrderTraverse(rootNode));
        //TODO:AccessibilityNodeInfo is not serializable

        if(sourceNode.getPackageName() == null){
            featurePack.packageName = "NULL";
        }
        else
            featurePack.packageName = sourceNode.getPackageName().toString();

        if(sourceNode.getClassName() == null){
            featurePack.className = "NULL";
        }
        else
            featurePack.className = sourceNode.getClassName().toString();

        if(sourceNode.getText() == null){
            featurePack.text = "NULL";
        }
        else
            featurePack.text = sourceNode.getText().toString();

        if(sourceNode.getContentDescription() == null){
            featurePack.contentDescription = "NULL";
        }
        else
            featurePack.contentDescription = sourceNode.getContentDescription().toString();

        if(sourceNode.getViewIdResourceName() == null){
            featurePack.viewId = "NULL";
        }
        else
            featurePack.viewId = sourceNode.getViewIdResourceName();

        featurePack.boundsInParent = boundsInParents.flattenToString();
        featurePack.boundsInScreen = boundsInScreen.flattenToString();
        featurePack.time = Calendar.getInstance().getTimeInMillis();
        featurePack.eventType = event.getEventType();
        featurePack.parentNode = new SerializableNodeInfo(parentNode);
        featurePack.childNodes = new AccessibilityNodeInfoList(childrenNodes).getSerializableList();
        featurePack.allNodes = new AccessibilityNodeInfoList(allNodes).getSerializableList();
        featurePack.isEditable = sourceNode.isEditable();
        featurePack.screenshot = screenshot;

        return featurePack;


    }

    @Deprecated
    private Intent generatePopUpActivityIntentFromEvent(AccessibilityEvent event, AccessibilityNodeInfo rootNode, File screenshot, HashSet<Map.Entry<String, String>> entryHashSet){
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        sourceNode.getBoundsInParent(boundsInParents);
        sourceNode.getBoundsInScreen(boundsInScreen);
        AccessibilityNodeInfo parentNode = sourceNode.getParent();
        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = null;
        if(sourceNode != null && Automator.preOrderTraverse(sourceNode) != null)
             childrenNodes = new ArrayList<>(Automator.preOrderTraverse(sourceNode));
        else
            childrenNodes = new ArrayList<>();
        ArrayList<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        if(rootNode != null)
             allNodes = new ArrayList<>(Automator.preOrderTraverse(rootNode));
        //TODO:AccessibilityNodeInfo is not serializable

        //pop up the selection window
        Intent popUpIntent = new Intent(this, mRecordingPopUpActivity.class);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        popUpIntent.putExtra("packageName", sourceNode.getPackageName());
        popUpIntent.putExtra("className", sourceNode.getClassName());
        popUpIntent.putExtra("text", sourceNode.getText());
        popUpIntent.putExtra("contentDescription", sourceNode.getContentDescription());
        popUpIntent.putExtra("viewId", sourceNode.getViewIdResourceName());
        popUpIntent.putExtra("boundsInParent", boundsInParents.flattenToString());
        popUpIntent.putExtra("boundsInScreen", boundsInScreen.flattenToString());
        popUpIntent.putExtra("time", Calendar.getInstance().getTimeInMillis());
        popUpIntent.putExtra("eventType", event.getEventType());
        popUpIntent.putExtra("parentNode", parentNode);
        popUpIntent.putExtra("childrenNodes", new AccessibilityNodeInfoList(childrenNodes));
        popUpIntent.putExtra("allNodes", new AccessibilityNodeInfoList(allNodes));
        popUpIntent.putExtra("isEditable", sourceNode.isEditable());
        popUpIntent.putExtra("screenshot", screenshot);
        popUpIntent.putExtra("trigger", mRecordingPopUpActivity.TRIGGERED_BY_NEW_EVENT);
        popUpIntent.putExtra("alternativeLabels", entryHashSet);
        return popUpIntent;
    }

    private HashSet<Map.Entry<String, String>> getAlternativeLabels (AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode){
        HashSet<Map.Entry<String, String>> retMap = new HashSet<>();
        List<AccessibilityNodeInfo> allNodes = Automator.preOrderTraverse(rootNode);
        if(allNodes == null)
            return retMap;
        for(AccessibilityNodeInfo node : allNodes){
            if(exceptedPackages.contains(node.getPackageName()))
                continue;
            if(!node.isClickable())
                continue;
            if(!(sourceNode == null || (sourceNode.getClassName() == null && node.getClassName() == null) || (sourceNode.getClassName() != null && node.getClassName() != null && sourceNode.getClassName().toString().contentEquals(node.getClassName()))))
                continue;
            if(node.getText() != null)
                retMap.add(new AbstractMap.SimpleEntry<>("Text", node.getText().toString()));
            List<AccessibilityNodeInfo> childNodes = Automator.preOrderTraverse(node);
            if(childNodes == null)
                continue;
            for(AccessibilityNodeInfo childNode : childNodes){
                if(childNode == null)
                    continue;
                if(childNode.getText() != null)
                    retMap.add(new AbstractMap.SimpleEntry<>("Child Text", childNode.getText().toString()));
            }
        }
        /*
        AccessibilityNodeInfo parentNode = sourceNode.getParent();
        if(parentNode == null)
            return retMap;
        AccessibilityNodeInfo grandParentNode = parentNode.getParent();
        if(grandParentNode == null) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                AccessibilityNodeInfo node = parentNode.getChild(i);
                if (node == null)
                    continue;
                if (node.getText() != null) {
                    retMap.add(new AbstractMap.SimpleEntry<>("Text", node.getText().toString()));
                }
                for (int j = 0; j < node.getChildCount(); j++) {
                    AccessibilityNodeInfo childNode = node.getChild(j);
                    if (childNode == null)
                        continue;
                    if (childNode.getText() != null) {
                        retMap.add(new AbstractMap.SimpleEntry<>("Child Text", childNode.getText().toString()));
                    }
                }
            }
        }
        else {
            for(int i = 0; i < grandParentNode.getChildCount(); i++){
                AccessibilityNodeInfo pNode = grandParentNode.getChild(i);
                if(pNode == null)
                    continue;
                for(int j = 0; j < pNode.getChildCount(); j++){
                    AccessibilityNodeInfo sNode = pNode.getChild(j);
                    if(sNode == null)
                        continue;
                    if (sNode.getText() != null) {
                        retMap.add(new AbstractMap.SimpleEntry<>("Text", sNode.getText().toString()));
                    }
                    for(int p = 0; p < sNode.getChildCount(); p ++){
                        AccessibilityNodeInfo cNode = sNode.getChild(p);
                        if(cNode == null)
                            continue;
                        if (cNode.getText() != null) {
                            retMap.add(new AbstractMap.SimpleEntry<>("Child Text", cNode.getText().toString()));
                        }
                    }
                }
            }
        }
        */
        return retMap;
    }
}

