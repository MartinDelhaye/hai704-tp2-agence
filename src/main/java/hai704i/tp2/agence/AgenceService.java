package hai704i.tp2.agence;

import hai704i.tp2.agence.generated.*;
import jakarta.xml.ws.WebServiceException;

import java.util.*;
import java.util.concurrent.*;

public class AgenceService {

    public record OffreHotel(HotelClient hotel, Offre offre) {}
    public record Resultat(List<OffreHotel> offres, List<String> incidents) {}

    private final List<HotelClient> hotels;

    public AgenceService(List<HotelClient> hotels) {
        this.hotels = List.copyOf(hotels);
    }

    public Resultat rechercher(DemandeRecherche demande) throws InterruptedException {
        List<OffreHotel> offres = new ArrayList<>();
        List<String> incidents = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(hotels.size());
        try {
            Map<HotelClient, Future<List<Offre>>> futurs = new LinkedHashMap<>();
            for (HotelClient h : hotels) {
                futurs.put(h, pool.submit(() -> h.chercher(demande)));
            }
            for (Map.Entry<HotelClient, Future<List<Offre>>> e : futurs.entrySet()) {
                try {
                    for (Offre o : e.getValue().get()) offres.add(new OffreHotel(e.getKey(), o));
                } catch (ExecutionException ex) {
                    incidents.add(decrire(e.getKey(), ex.getCause()));
                }
            }
        } finally {
            pool.shutdownNow();
        }

        offres.sort(Comparator
                .comparing((OffreHotel o) -> o.offre().getPrixTotal())
                .thenComparing(o -> -o.offre().getEtoiles())
                .thenComparing(o -> o.offre().getHotel()));
        return new Resultat(offres, incidents);
    }

    private static String decrire(HotelClient h, Throwable cause) {
        if (cause instanceof AgenceInconnueFaultException)
            return h.adresse() + " : agence non reconnue par cet hôtel";
        if (cause instanceof DemandeInvalideFaultException)
            return h.adresse() + " : demande refusée (" + cause.getMessage() + ")";
        if (cause instanceof WebServiceException)
            return h.adresse() + " : hôtel injoignable ou trop lent, ses offres sont ignorées";
        return h.adresse() + " : erreur inattendue (" + cause.getClass().getSimpleName() + ")";
    }
}