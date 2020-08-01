package edu.cmu.hcii.sugilite.automation;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by user on 2018-10-24.
 * Author: Jeongmin - Lumos project / KAIST
 * Purpose
 *  1) to notify the first UI interaction to dynamic packet learner - learning start
 *  2) to notify resource-ids to dynamic packet learner before UI interaction - learning phase
 *  3) to notify the end of UI interaction to dynamic packet learner - learning finish
 */

public class InteractionNotifier {

    private String learnerAddr = "192.168.0.205";
    private String uniqueIdentifier  = "Lumos_Dynamic";
    //opcode - 0:start 1:end 2:resid

    public void setMaincont(Context maincont) {
        this.maincont = maincont;
    }

    private Context maincont;


    private String makeRequest(String opcode, String resid)
    {
        if (resid != null)
            return learnerAddr + "?" + "uniqueIdentifier=" + uniqueIdentifier + "&opcode=" + opcode + "&resid=" + resid;
        return learnerAddr + "?" + "uniqueIdentifier=" + uniqueIdentifier + "&opcode=" + opcode;
    }

    private void sendRequest(String url)
    {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this.maincont);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
//                        mTextView.setText("Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                mTextView.setText("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void start_interaction()
    {
        sendRequest(makeRequest("0", null));
    }

    public void end_interaction()
    {
        sendRequest(makeRequest("1", null));
    }

    public void send_resourceid(String resid)
    {
        sendRequest(makeRequest("2", resid));
    }
}
