package hai704i.tp2.agence;

import hai704i.tp2.agence.generated.*;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AgenceMain {
    private static final int DELAI_CONNEXION_MS = 2_000;
    private static final int DELAI_REPONSE_MS = 5_000;

    public static void main(String[] args)
            throws Exception {
        String agence = args.length > 0 ? args[0] : "AG-01";
        String secret = args.length > 1 ? args[1] : "";
        List<String> adresses = args.length > 2
                ? List.of(args).subList(2, args.length)
                : List.of("http://localhost:8081/hotel", "http://localhost:8082/hotel");

        List<HotelClient> hotels = adresses.stream()
                .map(a -> new HotelClient(a, DELAI_CONNEXION_MS, DELAI_REPONSE_MS))
                .toList();
        AgenceService agenceService = new AgenceService(hotels);

        long debut = System.nanoTime();
        AgenceService.Resultat resultat = agenceService.rechercher(demandeRecherche(agence));
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;
        System.out.println("(recherche terminée en " + dureeMs + " ms)");
        resultat.incidents().forEach(i -> System.out.println("/!\\ " + i));
        if (resultat.offres().isEmpty()) {
            System.out.println("Aucune offre.");
            return;
        }
        System.out.println(resultat.offres().size() + " offre(s), de la moins chère à la plus chère :");
        for (AgenceService.OffreHotel o : resultat.offres()) {
            Offre f = o.offre();
            System.out.printf("  %s | %s | %d* | %s EUR | %s%n",
                    f.getHotel(), f.getChambre(), f.getEtoiles(), f.getPrixTotal(), f.getId());
        }
        if (Boolean.getBoolean("rechercheSeule")) return;

        AgenceService.OffreHotel choisie = resultat.offres().get(0);
        String idDemande = UUID.randomUUID().toString();
        reserver(choisie.hotel(), idDemande, agence, secret, choisie.offre(), "1. réservation");
        reserver(choisie.hotel(), idDemande, agence, secret, choisie.offre(), "2. rejeu (même idDemande)");
        reserver(choisie.hotel(), UUID.randomUUID().toString(), agence, secret, choisie.offre(),
                "3. nouvelle demande sur la même offre");
    }

    private static void reserver(
            HotelClient hotel,
            String idDemande,
            String agence,
            String secret,
            Offre offre,
            String etape
    ) {
        System.out.println("--- " + etape);
        try {
            Confirmation c = hotel.reserver(demandeReservation(idDemande, agence, secret, offre.getId()));
            boolean memePrix = c.getPrixTotal().compareTo(offre.getPrixTotal()) == 0;
            System.out.println("Réservation " + c.getReferenceReservation() + " : " + c.getPrixTotal()
                    + " EUR" + (c.isRejeu() ? " (rejeu : déjà enregistrée)" : "")
                    + " / prix confirmé = prix annoncé : " + memePrix);
        } catch (AgenceNonAuthentifieeFaultException e) {
            System.out.println("Refus de l'hôtel (authentification) : " + e.getMessage());
        } catch (PaiementRefuseFaultException e) {
            System.out.println("Refus de l'hôtel (paiement) : " + e.getMessage());
        } catch (OffreIndisponibleFaultException e) {
            System.out.println("Refus de l'hôtel (offre) : " + e.getMessage());
        } catch (DemandeInvalideFaultException e) {
            System.out.println("Refus de l'hôtel (demande invalide) : " + e.getMessage());
        } catch (WebServiceException e) {
            System.out.println(e.getMessage());
        }
    }

    private static DemandeRecherche demandeRecherche(String agence)
            throws Exception {
        DatatypeFactory f = DatatypeFactory.newInstance();
        DemandeRecherche d = new DemandeRecherche();
        d.setAgence(agence);
        d.setVille("Montpellier");
        d.setArrivee(f.newXMLGregorianCalendar("2026-12-10"));
        d.setDepart(f.newXMLGregorianCalendar("2026-12-13"));
        d.setPersonnes(2);
        d.setEtoilesMin(3);
        d.setPrixMax(new BigDecimal("400.00"));
        return d;
    }

    private static DemandeReservation demandeReservation(
            String idDemande,
            String agence,
            String secret,
            String idOffre
    ) {
        IdentificationAgence id = new IdentificationAgence();
        id.setIdAgence(agence);
        id.setSecret(secret);

        Voyageur v = new Voyageur();
        v.setNom("Dupont");
        v.setPrenom("Alice");

        Paiement p = new Paiement();
        p.setNumeroCarte("4000000000000002");
        p.setMoisExpiration(12);
        p.setAnneeExpiration(2030);
        p.setCvc("123");

        DemandeReservation d = new DemandeReservation();
        d.setIdDemande(idDemande);
        d.setIdentification(id);
        d.setIdOffre(idOffre);
        d.setVoyageur(v);
        d.setPaiement(p);
        return d;
    }
}