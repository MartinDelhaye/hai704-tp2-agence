package hai704i.tp2.agence;

import hai704i.tp2.agence.generated.*;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AgenceMain {
    public static void main(String[] args) throws Exception {
        String adresse = args.length > 0 ? args[0] : "http://localhost:8081/hotel";
        String agence = args.length > 1 ? args[1] : "AG-01";
        String secret = args.length > 2 ? args[2] : "";

        HotelPort port = new HotelService().getHotelSoapPort();
        ((BindingProvider) port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, adresse);

        try {
            List<Offre> offres = port.chercherOffres(demandeRecherche(agence));
            if (offres.isEmpty()) {
                System.out.println("Aucune offre.");
                return;
            }
            Offre choisie = offres.get(0);
            System.out.println("Offre choisie : " + choisie.getId() + " / " + choisie.getChambre()
                    + " / " + choisie.getPrixTotal() + " EUR (prix annoncé)");

            String idDemande = UUID.randomUUID().toString();
            reserver(port, idDemande, agence, secret, choisie, "1. réservation");
            reserver(port, idDemande, agence, secret, choisie, "2. rejeu (même idDemande)");
            reserver(port, UUID.randomUUID().toString(), agence, secret, choisie,
                    "3. nouvelle demande sur la même offre");
        } catch (AgenceInconnueFaultException e) {
            System.out.println("Refus de l'hôtel (agence) : " + e.getMessage());
        } catch (DemandeInvalideFaultException e) {
            System.out.println("Refus de l'hôtel (demande invalide) : " + e.getMessage());
        } catch (WebServiceException e) {
            System.out.println("Panne technique : l'hôtel est injoignable ou ne répond pas ("
                    + e.getClass().getSimpleName() + ")");
        }
    }

    private static void reserver(HotelPort port, String idDemande, String agence, String secret,
                                 Offre offre, String etape) {
        System.out.println("--- " + etape);
        try {
            Confirmation c = port.reserver(demandeReservation(idDemande, agence, secret, offre.getId()));
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
        }
    }

    private static DemandeRecherche demandeRecherche(String agence) throws Exception {
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

    private static DemandeReservation demandeReservation(String idDemande, String agence,
                                                         String secret, String idOffre) {
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