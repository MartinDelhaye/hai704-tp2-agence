package hai704i.tp2.agence;

import hai704i.tp2.agence.generated.*;
import jakarta.xml.ws.BindingProvider;

import java.util.List;
import java.util.Map;

public class HotelClient {
    private final String adresse;
    private final HotelPort port;

    public HotelClient(String adresse, int delaiConnexionMs, int delaiReponseMs) {
        this.adresse = adresse;
        this.port = new HotelService().getHotelSoapPort();
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, adresse);
        ctx.put("com.sun.xml.ws.connect.timeout", delaiConnexionMs);
        ctx.put("com.sun.xml.ws.request.timeout", delaiReponseMs);
        ctx.put("jakarta.xml.ws.client.connectionTimeout", delaiConnexionMs);
        ctx.put("jakarta.xml.ws.client.receiveTimeout", delaiReponseMs);
    }

    public String adresse() { return adresse; }

    public List<Offre> chercher(DemandeRecherche d)
            throws AgenceInconnueFaultException, DemandeInvalideFaultException {
        return port.chercherOffres(d);
    }

    public Confirmation reserver(DemandeReservation d)
            throws AgenceNonAuthentifieeFaultException, DemandeInvalideFaultException,
            PaiementRefuseFaultException, OffreIndisponibleFaultException {
        return port.reserver(d);
    }
}