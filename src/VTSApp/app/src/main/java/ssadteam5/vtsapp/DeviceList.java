package ssadteam5.vtsapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.auth0.android.jwt.Claim;
import com.auth0.android.jwt.JWT;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class DeviceList extends Fragment
{
    View view;
    private String token;
    private DeviceFetchTask mFetchTask;
    private ArrayList<HashMap<String, String>> deviceDet = new ArrayList<>();

    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_device_list, container, false);
        token = getArguments().getString("token");

        JWT jwt = new JWT(token);
        Claim claim = jwt.getClaim("organisationId");
        String organisationId = claim.asString();
        client = new OkHttpClient();
        Request request = new Request.Builder().url("http://eyedentifyapps.com:8080/socket/device/message"+organisationId+"/websocket/").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();
        mFetchTask = new DeviceFetchTask(token);
        mFetchTask.execute((Void) null);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Devices");
    }
    private final class EchoWebSocketListener extends WebSocketListener
    {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response)
        {
            Log.d("Opening:", response.toString());

        }

        @Override
        public void onMessage(WebSocket webSocket, String text)
        {
            Log.d("Receiving : ",text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes)
        {
            Log.d("Receiving bytes : ",bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason)
        {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.d("Closing : ", code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response)
        {
            Log.d("Error : ",t.getMessage());
        }
    }
    public class DeviceFetchTask extends AsyncTask<Void, Void, Boolean>
    {
        private final String mToken;

        DeviceFetchTask(String token)
        {
            mToken = token;
        }
        @Override
        protected Boolean doInBackground(Void... params)
        {

            HttpURLConnection conn;
            try {

                String response = "";
                URL url = new URL("http://eyedentifyapps.com:8080/api/auth/device/all/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept","*/*");
                conn.setRequestProperty("Authorization","Bearer " + mToken);
                InputStream in = conn.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(in);
                int inputStreamData = inputStreamReader.read();
                while (inputStreamData != -1)
                {
                    char current = (char) inputStreamData;
                    inputStreamData = inputStreamReader.read();
                    response += current;
                }
                Log.d("resp",response);
                JSONObject obj=new JSONObject(response);
                JSONArray arr=obj.getJSONArray("deviceDTOS");
                for(int i=0;i<arr.length();i++)
                {
                    JSONObject ob=arr.getJSONObject(i);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("account",ob.getString("account"));
                    Log.d("account",ob.getString("account"));
                    map.put("name",ob.getString("name"));
                    map.put("description",ob.getString("description"));
                    deviceDet.add(map);
                }


            }

            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success)
        {
            ListAdapter adapter = new SimpleAdapter(getContext(), deviceDet, R.layout.list_item,
                    new String[] { "account", "name","description"  },
                    new int[] { R.id.account,R.id.name, R.id.description });
            ListView listView=(ListView) view.findViewById(R.id.listview);
            listView.setAdapter(adapter);
        }

        @Override
        protected void onCancelled()
        {
        }
    }
}

