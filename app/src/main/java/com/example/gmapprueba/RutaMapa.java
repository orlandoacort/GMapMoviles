package com.example.gmapprueba;


import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class RutaMapa extends AsyncTask<Void,Integer,Boolean> {
    private static final String TOAST_MSG = "Calculando ...";
    private static final String TOAST_MSG_ERR = "No es posible Graficar esa ruta por el momento";

    private Context context;
    private GoogleMap gMap;
    private String editDesde;
    private String editHasta;
    private final ArrayList<LatLng> lstLatLng = new ArrayList<LatLng>();

    public RutaMapa(Context context, GoogleMap gMap, String editDesde, String editHasta) {
        this.context = context;
        this.gMap = gMap;
        this.editDesde = editDesde;
        this.editHasta = editHasta;
    }

    @Override
    protected void onPreExecute(){
        Toast.makeText(context,TOAST_MSG, Toast.LENGTH_LONG).show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try{
            final StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/directions/xml?sensor=false&language=pt");
            url.append("&origin=");
            url.append(editDesde.replace(' ','+'));
            url.append("&destination=");
            url.append(editHasta.replace(' ','+'));
            url.append("&key=");
            url.append("AIzaSyCYeYhrXCEcSZ6UzAzoEMR2CO8VMp4aLg8");

            final InputStream stream = new URL(url.toString()).openStream();

            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setIgnoringComments(true);

            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            final Document document = documentBuilder.parse(stream);
            document.getDocumentElement().normalize();

            final String status = document.getElementsByTagName("status").item(0).getTextContent();

            if(!"OK".equals(status)){
                return false;
            }

            final Element elementLeg = (Element)document.getElementsByTagName("leg").item(0);
            final NodeList nodeListStep = elementLeg.getElementsByTagName("step");
            final int length = nodeListStep.getLength();

            for(int i=0; i<length;i++){
                final Node nodeStep = nodeListStep.item(i);

                if(nodeStep.getNodeType() == Node.ELEMENT_NODE){
                    final Element elementStep = (Element) nodeStep;
                    //Generar  Polyline
                    codificarPolyline(elementStep.getElementsByTagName("points").item(0).getTextContent());
                }
            }

            return true;

        }catch (final Exception e) {
            return false;
        }
    }

    private void codificarPolyline(final String puntoCodificado){
        int index = 0;
        int lat=0,lng=0;

        while(index<puntoCodificado.length()){
            int b,shift=0,result=0;

            do{
                b = puntoCodificado.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while(b >= 0x20);

            int dlat = ((result & 1)!=0? ~(result>>1):(result>>1));
            lat += dlat;
            shift = 0;
            result = 0;

            do{
                b = puntoCodificado.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while (b >= 0x20);

            int dlng = ((result & 1)!=0? ~(result>>1):(result>>1));
            lng += dlng;

            lstLatLng.add(new LatLng((double)lat/1E5,(double)lng/1E5));
        }
    }

    @Override
    protected void onPostExecute(final Boolean result){
        if(!result) {
            Toast.makeText(context, TOAST_MSG_ERR, Toast.LENGTH_LONG).show();
        }else{
            final PolylineOptions polylines = new PolylineOptions();
            polylines.color(Color.GREEN);

            for(final LatLng latLng : lstLatLng){
                polylines.add(latLng);
            }

            final MarkerOptions markerA = new MarkerOptions();
            markerA.position(lstLatLng.get(0));
            markerA.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

            final MarkerOptions markerB = new MarkerOptions();
            markerB.position(lstLatLng.get(lstLatLng.size()-1));
            markerB.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lstLatLng.get(0),14));
            gMap.addMarker(markerA);
            gMap.addPolyline(polylines);
            gMap.addMarker(markerB);
        }
    }

}
