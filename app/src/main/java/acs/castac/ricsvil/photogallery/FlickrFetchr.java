package acs.castac.ricsvil.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickerFetchr";

    private static final String API_KEY = "6186c6f9c9594e62bcb769c420cf795b";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/").
            buildUpon().
            appendQueryParameter("api_key", API_KEY).
            appendQueryParameter("format", "json").
            appendQueryParameter("nojsoncallback", "1").
            appendQueryParameter("extras", "url_s").build();

    public byte[] gerUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0 , bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }



    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(gerUrlBytes(urlSpec));
    }

    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }

        return uriBuilder.build().toString();
    }

    private List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();

        try{
            String jsonString = getUrlString(url);
            Log.i(TAG, "JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        } catch (JSONException tje){
            Log.e(TAG, "Failed to parse JSON", tje);
        }

        Log.i(TAG, "JSON: " + items.toString());
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonObject) throws IOException,JSONException{
        JSONObject photosJsonObject = jsonObject.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i = 0; i<photosJsonArray.length(); i++){
            photosJsonObject = photosJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setmId(photosJsonObject.getString("id"));
            item.setmCaption(photosJsonObject.getString("title"));

            if(!photosJsonObject.has("url_s")){
                continue;
            }

            item.setmUrl(photosJsonObject.getString("url_s"));
            items.add(item);

        }


    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENT_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD, query);
        Log.i(TAG, "URL " + url);
        return downloadGalleryItems(url);
    }

}
