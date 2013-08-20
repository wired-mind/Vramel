package com.nxttxn.vramel.components.xmlsecurity;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.xml.security.Init;
import org.apache.xml.security.exceptions.XMLSecurityException;

import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/16/13
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMLSignatureValidator implements Processor {
    protected final Logger logger = LoggerFactory.getLogger(XMLSignatureValidator.class);

    public XMLSignatureValidator() {
        this(true);
    }

    public XMLSignatureValidator(boolean removeSignature) {
        this.removeSignature = removeSignature;

        Init.init();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        InputStream is = exchange.getIn().getMandatoryBody(InputStream.class);
        Document signedDoc = exchange.getContext().getTypeConverter().convertTo(Document.class, exchange, is);

        exchange.getOut().setBody(signedDoc);
        checkSignature(signedDoc);
    }

    private boolean removeSignature = true;
    private boolean persistSignature = true;
    private SignatureProperties sigProps;

    public void setRemoveSignature(boolean remove) {
        this.removeSignature = remove;
    }

    public void setPersistSignature(boolean persist) {
        this.persistSignature = persist;
    }

    protected void checkSignature(Document doc) throws WSSecurityException {
        checkNotNull(doc);

        Element root = doc.getDocumentElement();
        Element signatureElement = getSignatureElement(root);
        if (signatureElement == null) {
            throwFault("XML Signature is not available", null);
        }

        String cryptoKey = null;
        String propKey = null;


        Crypto crypto = CryptoFactory.getInstance();
//        try {
//            CryptoLoader loader = new CryptoLoader();
//            crypto = loader.getCrypto(message, cryptoKey, propKey);
//        } catch (Exception ex) {
//            throwFault("Crypto can not be loaded", ex);
//        }
        boolean valid = false;
        Reference ref = null;
        try {
            XMLSignature signature = new XMLSignature(signatureElement, "", true);



            ref = getReference(signature);
            Element signedElement = validateReference(root, ref);
            if (signedElement.hasAttributeNS(null, "ID")) {
                signedElement.setIdAttributeNS(null, "ID", true);
            }

            // See also WSS4J SAMLUtil.getCredentialFromKeyInfo
            KeyInfo keyInfo = signature.getKeyInfo();

            X509Certificate cert = keyInfo.getX509Certificate();
            if (cert != null) {
                valid = signature.checkSignatureValue(cert);
            } else {
                PublicKey pk = keyInfo.getPublicKey();
                if (pk != null) {
                    valid = signature.checkSignatureValue(pk);
                }
            }

            // validate trust
            new TrustValidator().validateTrust(crypto, cert, keyInfo.getPublicKey());

        } catch (Exception ex) {
            throwFault("Signature validation failed", ex);
        }
        if (!valid) {
            throwFault("Signature validation failed", null);
        }
        if (removeSignature) {
            if (!isEnveloping(root)) {
                Element signedEl = getSignedElement(root, ref);
                signedEl.removeAttribute("ID");
                root.removeChild(signatureElement);
            } else {
                Element actualBody = getActualBody(root);
                Document newDoc = createDocument();
                newDoc.adoptNode(actualBody);
                root = actualBody;
            }
        }
    }

    private Document createDocument() {
        DocumentBuilder builder = null;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        try {
            builder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Couldn't find a DOM parser.", e);
        }

        return builder.newDocument();
    }

    private Element getActualBody(Element envelopingSigElement) {
        Element objectNode = getNode(envelopingSigElement, Constants.SignatureSpecNS, "Object", 0);
        if (objectNode == null) {
            throwFault("Object envelope is not available", null);
        }
        Element node = getFirstElement(objectNode);
        if (node == null) {
            throwFault("No signed data is found", null);
        }
        return node;

    }

    private Element getSignatureElement(Element sigParentElement) {
        if (isEnveloping(sigParentElement)) {
            return sigParentElement;
        }
        return getFirstChildWithName(sigParentElement, Constants.SignatureSpecNS, "Signature");
    }

    protected boolean isEnveloping(Element root) {
        return Constants.SignatureSpecNS.equals(root.getNamespaceURI())
                && "Signature".equals(root.getLocalName());
    }

    protected Reference getReference(XMLSignature sig) {
        int count = sig.getSignedInfo().getLength();
        if (count != 1) {
            throwFault("Multiple Signature Reference are not currently supported", null);
        }
        try {
            return sig.getSignedInfo().item(0);
        } catch (XMLSecurityException ex) {
            throwFault("Signature Reference is not available", ex);
        }
        return null;
    }

    protected Element validateReference(Element root, Reference ref) {
        boolean enveloped = false;

        String refId = ref.getURI();

        boolean validLocalRef = refId.startsWith("#") || refId.equalsIgnoreCase("");
        if (!validLocalRef) {
            throwFault("Only local Signature References are supported", null);
        }

        Element signedEl = getSignedElement(root, ref);
        if (signedEl != null) {
            enveloped = signedEl == root;
        } else {
            throwFault("Signature Reference ID is invalid", null);
        }


        Transforms transforms = null;
        try {
            transforms = ref.getTransforms();
        } catch (XMLSecurityException ex) {
            throwFault("Signature transforms can not be obtained", ex);
        }

        boolean c14TransformConfirmed = false;
        String c14TransformExpected = sigProps != null ? sigProps.getSignatureC14Transform() : null;
        boolean envelopedConfirmed = false;
        for (int i = 0; i < transforms.getLength(); i++) {
            try {
                Transform tr = transforms.item(i);
                if (Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(tr.getURI())) {
                    envelopedConfirmed = true;
                } else if (c14TransformExpected != null && c14TransformExpected.equals(tr.getURI())) {
                    c14TransformConfirmed = true;
                }
            } catch (Exception ex) {
                throwFault("Problem accessing Transform instance", ex);
            }
        }
        if (enveloped && !envelopedConfirmed) {
            throwFault("Only enveloped signatures are currently supported", null);
        }
        if (c14TransformExpected != null && !c14TransformConfirmed) {
            throwFault("Transform Canonicalization is not supported", null);
        }

        if (sigProps != null && sigProps.getSignatureDigestAlgo() != null) {
            Element dm =
                    getFirstChildWithName(ref.getElement(), Constants.SignatureSpecNS, "DigestMethod");
            if (dm != null && !dm.getAttribute("Algorithm").equals(
                    sigProps.getSignatureDigestAlgo())) {
                throwFault("Signature Digest Algorithm is not supported", null);
            }
        }
        return signedEl;
    }

    private Element getSignedElement(Element root, Reference ref) {
        if (ref.getURI().length() == 0) {
            return root;
        }
        String rootId = root.getAttribute("ID");
        String expectedID = ref.getURI().substring(1);

        if (!expectedID.equals(rootId)) {
            return findElementById(root, expectedID, true);
        } else {
            return root;
        }
    }

    /**
     * Returns the single element that contains an Id with value
     * <code>uri</code> and <code>namespace</code>. The Id can be either a wsu:Id or an Id
     * with no namespace. This is a replacement for a XPath Id lookup with the given namespace.
     * It's somewhat faster than XPath, and we do not deal with prefixes, just with the real
     * namespace URI
     * <p/>
     * If checkMultipleElements is true and there are multiple elements, we log a
     * warning and return null as this can be used to get around the signature checking.
     *
     * @param startNode             Where to start the search
     * @param value                 Value of the Id attribute
     * @param checkMultipleElements If true then go through the entire tree and return
     *                              null if there are multiple elements with the same Id
     * @return The found element if there was exactly one match, or
     *         <code>null</code> otherwise
     */
    private static Element findElementById(
            Node startNode, String value, boolean checkMultipleElements
    ) {
        //
        // Replace the formerly recursive implementation with a depth-first-loop lookup
        //
        Node startParent = startNode.getParentNode();
        Node processedNode = null;
        Element foundElement = null;
        String id = value;

        while (startNode != null) {
            // start node processing at this point
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element) startNode;
                // Try the wsu:Id first
                String attributeNS = se.getAttributeNS(WSConstants.WSU_NS, "Id");
                if ("".equals(attributeNS) || !id.equals(attributeNS)) {
                    attributeNS = se.getAttributeNS(null, "Id");
                }
                if ("".equals(attributeNS) || !id.equals(attributeNS)) {
                    attributeNS = se.getAttributeNS(null, "ID");
                }
                if (!"".equals(attributeNS) && id.equals(attributeNS)) {
                    if (!checkMultipleElements) {
                        return se;
                    } else if (foundElement == null) {
                        foundElement = se; // Continue searching to find duplicates
                    } else {
                        // Multiple elements with the same 'Id' attribute value
                        return null;
                    }
                }
            }

            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }
            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return foundElement;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return foundElement;
    }

    protected void throwFault(String error, Exception ex) {
        logger.warn(error);
        throw ex != null ? new XMLSignatureValidatorException(error, ex) : new XMLSignatureValidatorException(error);
    }

    public void setSignatureProperties(SignatureProperties properties) {
        this.sigProps = properties;
    }


    protected Element getNode(Element parent, String ns, String name, int index) {
        NodeList list = parent.getElementsByTagNameNS(ns, name);
        if (list != null && list.getLength() >= index + 1) {
            return (Element)list.item(index);
        }
        return null;
    }


    public static Element getFirstChildWithName(Element parent, String ns, String lp) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element e = (Element)n;
                String ens = (e.getNamespaceURI() == null) ? "" : e.getNamespaceURI();
                if (ns.equals(ens) && lp.equals(e.getLocalName())) {
                    return e;
                }
            }
        }
        return null;
    }

    public static Element getFirstElement(Node parent) {
           Node n = parent.getFirstChild();
           while (n != null && Node.ELEMENT_NODE != n.getNodeType()) {
               n = n.getNextSibling();
           }
           if (n == null) {
               return null;
           }
           return (Element)n;
       }

       public static Element getNextElement(Element el) {
           Node nd = el.getNextSibling();
           while (nd != null) {
               if (nd.getNodeType() == Node.ELEMENT_NODE) {
                   return (Element)nd;
               }
               nd = nd.getNextSibling();
           }
           return null;
       }



}
