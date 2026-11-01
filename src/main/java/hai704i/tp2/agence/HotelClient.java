package hai704i.tp2.agence;

import hai704i.tp2.agence.AgenceService.Carte;
import hai704i.tp2.agence.AgenceService.Critere;
import hai704i.tp2.agence.AgenceService.OffreAgence;
import hai704i.tp2.agence.AgenceService.PanneException;
import hai704i.tp2.agence.AgenceService.RefusException;
import hai704i.tp2.agence.AgenceService.RefusException.Motif;
import hai704i.tp2.agence.AgenceService.Reservation;
import hai704i.tp2.agence.generated.*;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public String adresse() {
        return adresse;
    }

    public List<OffreAgence> chercher(String agence, Critere c) throws RefusException, PanneException {
        DemandeRecherche d = new DemandeRecherche();
        d.setAgence(agence);
        d.setVille(c.ville());
        d.setArrivee(versXml(c.arrivee()));
        d.setDepart(versXml(c.depart()));
        d.setPersonnes(c.personnes());
        d.setEtoilesMin(c.etoilesMin());
        d.setPrixMax(c.prixMax());
        try {
            List<OffreAgence> res = new ArrayList<>();
            for (Offre o : port.chercherOffres(d)) {
                res.add(new OffreAgence(o.getId(), nvl(o.getHotel()), nvl(o.getChambre()), o.getEtoiles(),
                        o.getCapacite(), versDate(o.getArrivee()), versDate(o.getDepart()),
                        o.getPrixTotal(), adresse));
            }
            return res;
        } catch (AgenceInconnueFaultException e) {
            throw new RefusException(Motif.AGENCE_INCONNUE, e.getMessage());
        } catch (DemandeInvalideFaultException e) {
            throw new RefusException(Motif.DEMANDE_INVALIDE, e.getMessage());
        } catch (WebServiceException e) {
            throw new PanneException("Hôtel " + adresse + " injoignable ou trop lent", e);
        }
    }

    public Reservation reserver(String idDemande, String agence, String secret, OffreAgence offre,
                                AgenceService.Voyageur voyageur, Carte carte)
            throws RefusException, PanneException {
        IdentificationAgence id = new IdentificationAgence();
        id.setIdAgence(agence);
        id.setSecret(secret);

        Voyageur v = new Voyageur();
        v.setNom(voyageur.nom());
        v.setPrenom(voyageur.prenom());

        Paiement p = new Paiement();
        p.setNumeroCarte(carte.numero());
        p.setMoisExpiration(carte.mois());
        p.setAnneeExpiration(carte.annee());
        p.setCvc(carte.cvc());

        DemandeReservation d = new DemandeReservation();
        d.setIdDemande(idDemande);
        d.setIdentification(id);
        d.setIdOffre(offre.id());
        d.setVoyageur(v);
        d.setPaiement(p);
        try {
            Confirmation c = port.reserver(d);
            return new Reservation(c.getReferenceReservation(), nvl(c.getHotel()), nvl(c.getChambre()),
                    versDate(c.getArrivee()), versDate(c.getDepart()), c.getPrixTotal(), c.isRejeu());
        } catch (AgenceNonAuthentifieeFaultException e) {
            throw new RefusException(Motif.AUTHENTIFICATION, e.getMessage());
        } catch (PaiementRefuseFaultException e) {
            throw new RefusException(Motif.PAIEMENT, e.getMessage());
        } catch (OffreIndisponibleFaultException e) {
            throw new RefusException(Motif.OFFRE_INDISPONIBLE, e.getMessage());
        } catch (DemandeInvalideFaultException e) {
            throw new RefusException(Motif.DEMANDE_INVALIDE, e.getMessage());
        } catch (WebServiceException e) {
            throw new PanneException("Hôtel " + adresse + " injoignable ou trop lent", e);
        }
    }

    private static XMLGregorianCalendar versXml(LocalDate d) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(d.toString());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDate versDate(XMLGregorianCalendar x) {
        return x == null ? null : LocalDate.of(x.getYear(), x.getMonth(), x.getDay());
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}