package msdcrm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CrmAuth {

	/**
	 * Gets a CRM Online SOAP header & expiration.
	 * 
	 * @return CrmAuthenticationHeader An object containing the SOAP header and
	 *         expiration date/time of the header.
	 * @param username
	 *            Username of a valid CRM user.
	 * @param password
	 *            Password of a valid CRM user.
	 * @param url
	 *            The Url of the CRM Online organization
	 *            (https://org.crm.dynamics.com).).
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 */
	public CrmAuthenticationHeader GetHeaderOnline(String username,
			String password, String url) throws IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {

		if (!url.endsWith("/"))
			url += "/";

		String urnAddress = GetUrnOnline(url);
		Date now = new Date();

		StringBuilder xml = new StringBuilder();
		xml.append("<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">");
		xml.append("<s:Header>");
		xml.append("<a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue</a:Action>");
		xml.append("<a:MessageID>urn:uuid:" + java.util.UUID.randomUUID()
				+ "</a:MessageID>");
		xml.append("<a:ReplyTo>");
		xml.append("<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>");
		xml.append("</a:ReplyTo>");
		xml.append("<a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/RST2.srf</a:To>");
		xml.append("<o:Security s:mustUnderstand=\"1\" xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<u:Timestamp u:Id=\"_0\">");
		xml.append("<u:Created>" + String.format("%tFT%<tT.%<tLZ", now)
				+ "</u:Created>");
		xml.append("<u:Expires>"
				+ String.format("%tFT%<tT.%<tLZ", AddMinutes(60, now))
				+ "</u:Expires>");
		xml.append("</u:Timestamp>");
		xml.append("<o:UsernameToken u:Id=\"uuid-"
				+ java.util.UUID.randomUUID() + "-1\">");
		xml.append("<o:Username>" + username + "</o:Username>");
		xml.append("<o:Password>" + password + "</o:Password>");
		xml.append("</o:UsernameToken>");
		xml.append("</o:Security>");
		xml.append("</s:Header>");
		xml.append("<s:Body>");
		xml.append("<trust:RequestSecurityToken xmlns:trust=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">");
		xml.append("<wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">");
		xml.append("<a:EndpointReference>");
		xml.append("<a:Address>urn:" + urnAddress + "</a:Address>");
		xml.append("</a:EndpointReference>");
		xml.append("</wsp:AppliesTo>");
		xml.append("<trust:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</trust:RequestType>");
		xml.append("</trust:RequestSecurityToken>");
		xml.append("</s:Body>");
		xml.append("</s:Envelope>");

		URL LoginURL = new URL("https://login.microsoftonline.com/RST2.srf");
		HttpURLConnection rc = (HttpURLConnection) LoginURL.openConnection();

		rc.setRequestMethod("POST");
		rc.setDoOutput(true);
		rc.setDoInput(true);
		rc.setRequestProperty("Content-Type",
				"application/soap+xml; charset=UTF-8");
		String reqStr = xml.toString();
		int len = reqStr.length();
		rc.setRequestProperty("Content-Length", Integer.toString(len));
		rc.connect();
		OutputStreamWriter out = new OutputStreamWriter(rc.getOutputStream());
		out.write(reqStr, 0, len);
		out.flush();

		InputStreamReader read = new InputStreamReader(rc.getInputStream());
		StringBuilder sb = new StringBuilder();
		int ch = read.read();
		while (ch != -1) {
			sb.append((char) ch);
			ch = read.read();
		}
		String response = sb.toString();
		read.close();
		rc.disconnect();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document x = builder
				.parse(new ByteArrayInputStream(response.getBytes()));

		NodeList cipherElements = x.getElementsByTagName("CipherValue");
		String token1 = cipherElements.item(0).getTextContent();
		String token2 = cipherElements.item(1).getTextContent();

		NodeList keyIdentiferElements = x
				.getElementsByTagName("wsse:KeyIdentifier");
		String keyIdentifer = keyIdentiferElements.item(0).getTextContent();

		NodeList tokenExpiresElements = x.getElementsByTagName("wsu:Expires");
		String tokenExpires = tokenExpiresElements.item(0).getTextContent();

		Calendar c = DatatypeConverter.parseDateTime(tokenExpires);
		CrmAuthenticationHeader authHeader = new CrmAuthenticationHeader();
		authHeader.Expires = c.getTime();
		authHeader.Header = CreateSoapHeaderOnline(url, keyIdentifer, token1,
				token2);
		System.out.println("authHeader " + authHeader.Header);
		return authHeader;
	}

	/**
	 * Gets a CRM Online SOAP header.
	 * 
	 * @return String The XML SOAP header to be used in future requests.
	 * @param url
	 *            The Url of the CRM Online organization
	 *            (https://org.crm.dynamics.com).
	 * @param keyIdentifer
	 *            The KeyIdentifier from the initial request.
	 * @param token1
	 *            The first token from the initial request.
	 * @param token2
	 *            The second token from the initial request..
	 */
	public String CreateSoapHeaderOnline(String url, String keyIdentifer,
			String token1, String token2) {
		StringBuilder xml = new StringBuilder();
		xml.append("<s:Header>");
		xml.append("<a:Action s:mustUnderstand=\"1\">http://schemas.microsoft.com/xrm/2011/Contracts/Services/IOrganizationService/Execute</a:Action>");
		xml.append("<Security xmlns=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<EncryptedData Id=\"Assertion0\" Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\">");
		xml.append("<EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\"/>");
		xml.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">");
		xml.append("<EncryptedKey>");
		xml.append("<EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\"/>");
		xml.append("<ds:KeyInfo Id=\"keyinfo\">");
		xml.append("<wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<wsse:KeyIdentifier EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">"
				+ keyIdentifer + "</wsse:KeyIdentifier>");
		xml.append("</wsse:SecurityTokenReference>");
		xml.append("</ds:KeyInfo>");
		xml.append("<CipherData>");
		xml.append("<CipherValue>" + token1 + "</CipherValue>");
		xml.append("</CipherData>");
		xml.append("</EncryptedKey>");
		xml.append("</ds:KeyInfo>");
		xml.append("<CipherData>");
		xml.append("<CipherValue>" + token2 + "</CipherValue>");
		xml.append("</CipherData>");
		xml.append("</EncryptedData>");
		xml.append("</Security>");
		xml.append("<a:MessageID>urn:uuid:" + java.util.UUID.randomUUID()
				+ "</a:MessageID>");
		xml.append("<a:ReplyTo>");
		xml.append("<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>");
		xml.append("</a:ReplyTo>");
		xml.append("<a:To s:mustUnderstand=\"1\">" + url
				+ "XRMServices/2011/Organization.svc</a:To>");
		xml.append("</s:Header>");

		return xml.toString();
	}

	/**
	 * Gets the correct URN Address based on the Online region.
	 * 
	 * @return String URN Address.
	 * @param url
	 *            The Url of the CRM Online organization
	 *            (https://org.crm.dynamics.com).
	 */
	public String GetUrnOnline(String url) {
		if (url.toUpperCase().contains("CRM2.DYNAMICS.COM"))
			return "crmsam:dynamics.com";
		if (url.toUpperCase().contains("CRM4.DYNAMICS.COM"))
			return "crmemea:dynamics.com";
		if (url.toUpperCase().contains("CRM5.DYNAMICS.COM"))
			return "crmapac:dynamics.com";
		if (url.toUpperCase().contains("CRM6.DYNAMICS.COM"))
			return "crmoce:dynamics.com";
		if (url.toUpperCase().contains("CRM7.DYNAMICS.COM"))
			return "crmjpn:dynamics.com";
		if (url.toUpperCase().contains("CRM8.DYNAMICS.COM"))
			return "crmgcc:dynamics.com";

		return "crmna:dynamics.com";
	}

	/**
	 * Gets a CRM On Premise SOAP header & expiration.
	 * 
	 * @return CrmAuthenticationHeader An object containing the SOAP header and
	 *         expiration date/time of the header.
	 * @param username
	 *            Username of a valid CRM user.
	 * @param password
	 *            Password of a valid CRM user.
	 * @param url
	 *            The Url of the CRM On Premise (IFD) organization
	 *            (https://org.domain.com).
	 * @throws Exception
	 */
	public CrmAuthenticationHeader GetHeaderOnPremise(String username,
			String password, String url) throws Exception {

		if (!url.endsWith("/"))
			url += "/";
		String adfsUrl = GetAdfs(url);
		if (adfsUrl == null)
			return null;
		Date now = new Date();
		String urnAddress = url + "XRMServices/2011/Organization.svc";
		String usernamemixed = adfsUrl + "/13/usernamemixed";

		TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");
		formatter.setTimeZone(gmtTZ);

		StringBuilder xml = new StringBuilder();
		xml.append("<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\">");
		xml.append("<s:Header>");
		xml.append("<a:Action s:mustUnderstand=\"1\">http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue</a:Action>");
		xml.append("<a:MessageID>urn:uuid:" + java.util.UUID.randomUUID()
				+ "</a:MessageID>");
		xml.append("<a:ReplyTo>");
		xml.append("<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>");
		xml.append("</a:ReplyTo>");
		xml.append("<Security s:mustUnderstand=\"1\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<u:Timestamp  u:Id=\"" + java.util.UUID.randomUUID()
				+ "\">");
		xml.append("<u:Created>" + formatter.format(now) + "Z</u:Created>");
		xml.append("<u:Expires>" + formatter.format(AddMinutes(60, now))
				+ "Z</u:Expires>");
		xml.append("</u:Timestamp>");
		xml.append("<UsernameToken u:Id=\"" + java.util.UUID.randomUUID()
				+ "\">");
		xml.append("<Username>" + username + "</Username>");
		xml.append("<Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">"
				+ password + "</Password>");
		xml.append("</UsernameToken>");
		xml.append("</Security>");
		xml.append("<a:To s:mustUnderstand=\"1\">" + usernamemixed + "</a:To>");
		xml.append("</s:Header>");
		xml.append("<s:Body>");
		xml.append("<trust:RequestSecurityToken xmlns:trust=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">");
		xml.append("<wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">");
		xml.append("<a:EndpointReference>");
		xml.append("<a:Address>" + urnAddress + "</a:Address>");
		xml.append("</a:EndpointReference>");
		xml.append("</wsp:AppliesTo>");
		xml.append("<trust:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</trust:RequestType>");
		xml.append("</trust:RequestSecurityToken>");
		xml.append("</s:Body>");
		xml.append("</s:Envelope>");

		URL mexURL = new URL(usernamemixed);
		HttpURLConnection rc = (HttpURLConnection) mexURL.openConnection();

		rc.setRequestMethod("POST");
		rc.setDoOutput(true);
		rc.setDoInput(true);
		rc.setRequestProperty("Content-Type",
				"application/soap+xml; charset=UTF-8");
		String reqStr = xml.toString();
		int len = reqStr.length();
		rc.setRequestProperty("Content-Length", Integer.toString(len));
		rc.connect();
		OutputStreamWriter out = new OutputStreamWriter(rc.getOutputStream());
		out.write(reqStr, 0, len);
		out.flush();

		InputStreamReader read = new InputStreamReader(rc.getInputStream());
		StringBuilder sb = new StringBuilder();
		int ch = read.read();
		while (ch != -1) {
			sb.append((char) ch);
			ch = read.read();
		}
		String response = sb.toString();
		read.close();
		rc.disconnect();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document x = builder
				.parse(new ByteArrayInputStream(response.getBytes()));

		NodeList cipherValue1 = x.getElementsByTagName("e:CipherValue");
		String token1 = cipherValue1.item(0).getFirstChild().getTextContent();

		NodeList cipherValue2 = x.getElementsByTagName("xenc:CipherValue");
		String token2 = cipherValue2.item(0).getFirstChild().getTextContent();

		NodeList keyIdentiferElements = x
				.getElementsByTagName("o:KeyIdentifier");
		String keyIdentifer = keyIdentiferElements.item(0).getFirstChild()
				.getTextContent();

		NodeList x509IssuerNameElements = x
				.getElementsByTagName("X509IssuerName");
		String x509IssuerName = x509IssuerNameElements.item(0).getFirstChild()
				.getTextContent();

		NodeList x509SerialNumberElements = x
				.getElementsByTagName("X509SerialNumber");
		String x509SerialNumber = x509SerialNumberElements.item(0)
				.getFirstChild().getTextContent();

		NodeList binarySecretElements = x
				.getElementsByTagName("trust:BinarySecret");
		String binarySecret = binarySecretElements.item(0).getFirstChild()
				.getTextContent();

		String created = formatter.format(AddMinutes(-1, now)) + "Z";
		String expires = formatter.format(AddMinutes(60, now)) + "Z";
		String timestamp = "<u:Timestamp xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" u:Id=\"_0\"><u:Created>"
				+ created
				+ "</u:Created><u:Expires>"
				+ expires
				+ "</u:Expires></u:Timestamp>";

		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] hashedDataBytes = mDigest.digest(timestamp.getBytes());
		String digestValue = Base64.encodeBase64String(hashedDataBytes);

		String signedInfo = "<SignedInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></CanonicalizationMethod><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"></SignatureMethod><Reference URI=\"#_0\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></Transform></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod><DigestValue>"
				+ digestValue + "</DigestValue></Reference></SignedInfo>";
		byte[] signedInfoBytes = signedInfo.getBytes("UTF-8");
		Mac hmac = Mac.getInstance("HmacSHA1");
		byte[] binarySecretBytes = Base64.decodeBase64(binarySecret);
		SecretKeySpec key = new SecretKeySpec(binarySecretBytes, "HmacSHA1");
		hmac.init(key);
		byte[] hmacHash = hmac.doFinal(signedInfoBytes);
		String signatureValue = Base64.encodeBase64String(hmacHash);

		NodeList tokenExpiresElements = x.getElementsByTagName("wsu:Expires");
		String tokenExpires = tokenExpiresElements.item(0).getTextContent();

		Calendar c = DatatypeConverter.parseDateTime(tokenExpires);
		CrmAuthenticationHeader authHeader = new CrmAuthenticationHeader();
		authHeader.Expires = c.getTime();
		authHeader.Header = CreateSoapHeaderOnPremise(url, keyIdentifer,
				token1, token2, x509IssuerName, x509SerialNumber,
				signatureValue, digestValue, created, expires);

		return authHeader;
	}

	/**
	 * Gets a CRM On Premise (IFD) SOAP header.
	 * 
	 * @return String SOAP Header XML.
	 * @param url
	 *            The Url of the CRM On Premise (IFD) organization
	 *            (https://org.domain.com).
	 * @param keyIdentifer
	 *            The KeyIdentifier from the initial request.
	 * @param token1
	 *            The first token from the initial request.
	 * @param token2
	 *            The second token from the initial request.
	 * @param issuerNameX509
	 *            The certificate issuer.
	 * @param serialNumberX509
	 *            The certificate serial number.
	 * @param signatureValue
	 *            The hashsed value of the header signature.
	 * @param digestValue
	 *            The hashed value of the header timestamp.
	 * @param created
	 *            The header created date/time..
	 * @param expires
	 *            The header expiration date/tim.
	 */
	public String CreateSoapHeaderOnPremise(String url, String keyIdentifer,
			String token1, String token2, String issuerNameX509,
			String serialNumberX509, String signatureValue, String digestValue,
			String created, String expires) {
		StringBuilder xml = new StringBuilder();
		xml.append("<s:Header>");
		xml.append("<a:Action s:mustUnderstand=\"1\">http://schemas.microsoft.com/xrm/2011/Contracts/Services/IOrganizationService/Execute</a:Action>");
		xml.append("<a:MessageID>urn:uuid:" + java.util.UUID.randomUUID()
				+ "</a:MessageID>");
		xml.append("<a:ReplyTo>");
		xml.append("<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>");
		xml.append("</a:ReplyTo>");
		xml.append("<a:To s:mustUnderstand=\"1\">" + url
				+ "XRMServices/2011/Organization.svc</a:To>");
		xml.append("<o:Security xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<u:Timestamp xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" u:Id=\"_0\">");
		xml.append("<u:Created>" + created + "</u:Created>");
		xml.append("<u:Expires>" + expires + "</u:Expires>");
		xml.append("</u:Timestamp>");
		xml.append("<xenc:EncryptedData Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">");
		xml.append("<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>");
		xml.append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
		xml.append("<e:EncryptedKey xmlns:e=\"http://www.w3.org/2001/04/xmlenc#\">");
		xml.append("<e:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\">");
		xml.append("<DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>");
		xml.append("</e:EncryptionMethod>");
		xml.append("<KeyInfo>");
		xml.append("<o:SecurityTokenReference xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<X509Data>");
		xml.append("<X509IssuerSerial>");
		xml.append("<X509IssuerName>" + issuerNameX509 + "</X509IssuerName>");
		xml.append("<X509SerialNumber>" + serialNumberX509
				+ "</X509SerialNumber>");
		xml.append("</X509IssuerSerial>");
		xml.append("</X509Data>");
		xml.append("</o:SecurityTokenReference>");
		xml.append("</KeyInfo>");
		xml.append("<e:CipherData>");
		xml.append("<e:CipherValue>" + token1 + "</e:CipherValue>");
		xml.append("</e:CipherData>");
		xml.append("</e:EncryptedKey>");
		xml.append("</KeyInfo>");
		xml.append("<xenc:CipherData>");
		xml.append("<xenc:CipherValue>" + token2 + "</xenc:CipherValue>");
		xml.append("</xenc:CipherData>");
		xml.append("</xenc:EncryptedData>");
		xml.append("<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
		xml.append("<SignedInfo>");
		xml.append("<CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>");
		xml.append("<SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"/>");
		xml.append("<Reference URI=\"#_0\">");
		xml.append("<Transforms>");
		xml.append("<Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>");
		xml.append("</Transforms>");
		xml.append("<DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>");
		xml.append("<DigestValue>" + digestValue + "</DigestValue>");
		xml.append("</Reference>");
		xml.append("</SignedInfo>");
		xml.append("<SignatureValue>" + signatureValue + "</SignatureValue>");
		xml.append("<KeyInfo>");
		xml.append("<o:SecurityTokenReference xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		xml.append("<o:KeyIdentifier ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID\">"
				+ keyIdentifer + "</o:KeyIdentifier>");
		xml.append("</o:SecurityTokenReference>");
		xml.append("</KeyInfo>");
		xml.append("</Signature>");
		xml.append("</o:Security>");
		xml.append("</s:Header>");

		return xml.toString();
	}

	/**
	 * Gets the name of the ADFS server CRM uses for authentication.
	 * 
	 * @return String The AD FS server url.
	 * @param url
	 *            The Url of the CRM On Premise (IFD) organization
	 *            (https://org.domain.com).
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static String GetAdfs(String url) throws IOException,
			ParserConfigurationException, SAXException {
		URL WsdlURL = new URL(url
				+ "/XrmServices/2011/Organization.svc?wsdl=wsdl0");
		HttpURLConnection rc = (HttpURLConnection) WsdlURL.openConnection();

		rc.setRequestMethod("GET");
		rc.setDoOutput(true);

		InputStreamReader read = new InputStreamReader(rc.getInputStream());
		StringBuilder sb = new StringBuilder();
		int ch = read.read();
		while (ch != -1) {
			sb.append((char) ch);
			ch = read.read();
		}
		String response = sb.toString();
		read.close();
		rc.disconnect();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document x = builder
				.parse(new ByteArrayInputStream(response.getBytes()));

		NodeList nodes = x.getElementsByTagName("ms-xrm:Identifier");
		if (nodes.getLength() == 0)
			return null;

		return nodes.item(0).getFirstChild().getTextContent()
				.replace("http://", "https://");
	}

	/**
	 * 
	 * @return Date The date with added minutes.
	 * @param minutes
	 *            Number of minutes to add.
	 * @param time
	 *            Date to add minutes to.
	 */
	private Date AddMinutes(int minutes, Date time) {
		long ONE_MINUTE_IN_MILLIS = 60000;
		long currentTime = time.getTime();
		Date newDate = new Date(currentTime + (minutes * ONE_MINUTE_IN_MILLIS));
		return newDate;
	}
}
