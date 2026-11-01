package hai704i.tp2.agence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Logique de l'agence. Aucune classe engendrée ici : la console ne manipule que les types ci-dessous. */
public class AgenceService {

    // ---------- types propres à l'agence ----------
    public record Critere(String ville, LocalDate arrivee, LocalDate depart,
                          int personnes, int etoilesMin, BigDecimal prixMax) {}

    /** source = adresse de l'hôtel d'origine (référence opaque, sert à réserver au bon endroit). */
    public record OffreAgence(String id, String hotel, String chambre, int etoiles, int capacite,
                              LocalDate arrivee, LocalDate depart, BigDecimal prixTotal, String source) {}

    public record Voyageur(String nom, String prenom) {}

    /** Le numéro et le CVC ne sont jamais affichés : toString ne montre que les 4 derniers chiffres. */
    public record Carte(String numero, int mois, int annee, String cvc) {
        public String numeroMasque() { return "**** " + numero.substring(numero.length() - 4); }
        @Override public String toString() { return "Carte[" + numeroMasque() + "]"; }
    }

    public record Reservation(String reference, String hotel, String chambre, LocalDate arrivee,
                              LocalDate depart, BigDecimal prixTotal, boolean rejeu) {}

    public record Resultat(List<OffreAgence> offres, List<String> incidents) {}

    /** L'hôtel a répondu et refusé : réessayer à l'identique ne changera rien. */
    public static class RefusException extends Exception {
        public enum Motif { AGENCE_INCONNUE, AUTHENTIFICATION, PAIEMENT, OFFRE_INDISPONIBLE, DEMANDE_INVALIDE }
        private final Motif motif;
        public RefusException(Motif motif, String detail) {
            super(detail == null ? "" : detail);
            this.motif = motif;
        }
        public Motif motif() { return motif; }
        /** Phrase lisible : ce que l'hôtel a refusé et quoi faire ensuite. */
        public String explication() {
            String d = getMessage().isBlank() ? "" : " (" + getMessage() + ")";
            return switch (motif) {
                case AGENCE_INCONNUE -> "L'hôtel ne reconnaît pas l'agence comme partenaire" + d + ".";
                case AUTHENTIFICATION -> "L'hôtel n'a pas pu authentifier l'agence" + d
                        + ". Vérifiez l'identifiant et le secret de lancement.";
                case PAIEMENT -> "Paiement refusé" + d + ". Vérifiez la carte puis recommencez.";
                case OFFRE_INDISPONIBLE -> "Cette offre n'est plus disponible" + d
                        + ". Relancez une recherche.";
                case DEMANDE_INVALIDE -> "Demande jugée invalide par l'hôtel" + d + ".";
            };
        }
    }

    /** Panne technique : hôtel éteint, adresse erronée, réseau coupé ou réponse trop lente. */
    public static class PanneException extends Exception {
        public PanneException(String message, Throwable cause) { super(message, cause); }
    }

    // ---------- service ----------
    private final String agence;
    private final String secret;
    private final List<HotelClient> hotels;
    private final Map<String, HotelClient> parAdresse = new HashMap<>();

    public AgenceService(String agence, String secret, List<HotelClient> hotels) {
        if (hotels.isEmpty()) throw new IllegalArgumentException("aucun hôtel configuré");
        this.agence = agence;
        this.secret = secret;
        this.hotels = List.copyOf(hotels);
        for (HotelClient h : hotels) parAdresse.put(h.adresse(), h);
    }

    public String identifiant() { return agence; }

    public static String nouvelIdDemande() { return UUID.randomUUID().toString(); }

    public Resultat rechercher(Critere critere) throws InterruptedException {
        List<OffreAgence> offres = new ArrayList<>();
        List<String> incidents = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(hotels.size());
        try {
            Map<HotelClient, Future<List<OffreAgence>>> futurs = new LinkedHashMap<>();
            for (HotelClient h : hotels) {
                futurs.put(h, pool.submit(() -> h.chercher(agence, critere)));
            }
            for (Map.Entry<HotelClient, Future<List<OffreAgence>>> e : futurs.entrySet()) {
                try {
                    offres.addAll(e.getValue().get());
                } catch (ExecutionException ex) {
                    incidents.add(decrire(e.getKey(), ex.getCause()));
                }
            }
        } finally {
            pool.shutdownNow();
        }

        offres.sort(Comparator.comparing(OffreAgence::prixTotal)
                .thenComparing(OffreAgence::etoiles, Comparator.reverseOrder())
                .thenComparing(OffreAgence::hotel));
        return new Resultat(List.copyOf(offres), List.copyOf(incidents));
    }

    /** idDemande fourni par l'appelant : réessayer après une panne réutilise le même, donc jamais deux réservations. */
    public Reservation reserver(String idDemande, OffreAgence offre, Voyageur voyageur, Carte carte)
            throws RefusException, PanneException {
        HotelClient hotel = parAdresse.get(offre.source());
        return hotel.reserver(idDemande, agence, secret, offre, voyageur, carte);
    }

    private static String decrire(HotelClient h, Throwable cause) {
        if (cause instanceof RefusException r) return h.adresse() + " : " + r.explication();
        if (cause instanceof PanneException)
            return h.adresse() + " : hôtel injoignable ou trop lent, ses offres sont ignorées";
        return h.adresse() + " : erreur inattendue (" + cause.getClass().getSimpleName() + ")";
    }
}