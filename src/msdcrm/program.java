package msdcrm;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class program {

	public static void main(String[] args) throws Exception {

		CrmAuth auth = new CrmAuth();

		String url = args[0];
		String username =args[1]; 
		String password = args[2];
		
		// CRM Online
//		CrmAuthenticationHeader authHeader = auth.GetHeaderOnline(username,		password, url);
		// End CRM Online

		// CRM OnPremise - IFD
		//String url = "https://org.domain.com/";
		//// Username format could be domain\\username or username in the form of an email
		//String username = "username";
		//String password = "password";
		CrmAuthenticationHeader authHeader = auth.GetHeaderOnPremise(username, password, url);
		// End CRM OnPremise - IFD

		String id = CrmWhoAmI(authHeader, url);
		if (id == null)
			return;

//		String name = CrmGetUserName(authHeader, id, url);
		CrmGetOpportunityCount(authHeader, id, url);
//		System.out.println(name);
	}

	public static String CrmWhoAmI(CrmAuthenticationHeader authHeader,
			String url) throws IOException, SAXException,
			ParserConfigurationException {
		StringBuilder xml = new StringBuilder();
		xml.append("<s:Body>");
		xml.append("<Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\">");
		xml.append("<request i:type=\"c:WhoAmIRequest\" xmlns:b=\"http://schemas.microsoft.com/xrm/2011/Contracts\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:c=\"http://schemas.microsoft.com/crm/2011/Contracts\">");
		xml.append("<b:Parameters xmlns:d=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\"/>");
		xml.append("<b:RequestId i:nil=\"true\"/>");
		xml.append("<b:RequestName>WhoAmI</b:RequestName>");
		xml.append("</request>");
		xml.append("</Execute>");
		xml.append("</s:Body>");

		Document xDoc = CrmExecuteSoap.ExecuteSoapRequest(authHeader,
				xml.toString(), url);
		if (xDoc == null)
			return null;

		NodeList nodes = xDoc
				.getElementsByTagName("b:KeyValuePairOfstringanyType");
		for (int i = 0; i < nodes.getLength(); i++) {
			if ((nodes.item(i).getFirstChild().getTextContent())
					.equals("UserId")) {
				return nodes.item(i).getLastChild().getTextContent();
			}
		}

		return null;
	}

	public static String CrmGetUserName(CrmAuthenticationHeader authHeader,
			String id, String url) throws IOException, SAXException,
			ParserConfigurationException {
		System.out.println("url ");
		StringBuilder xml = new StringBuilder();
		xml.append("<s:Body>");
		xml.append("<Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
		xml.append("<request i:type=\"a:RetrieveRequest\" xmlns:a=\"http://schemas.microsoft.com/xrm/2011/Contracts\">");
		xml.append("<a:Parameters xmlns:b=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\">");
		xml.append("<a:KeyValuePairOfstringanyType>");
		xml.append("<b:key>Target</b:key>");
		xml.append("<b:value i:type=\"a:EntityReference\">");
		xml.append("<a:Id>" + id + "</a:Id>");
		xml.append("<a:LogicalName>systemuser</a:LogicalName>");
		xml.append("<a:Name i:nil=\"true\" />");
		xml.append("</b:value>");
		xml.append("</a:KeyValuePairOfstringanyType>");
		xml.append("<a:KeyValuePairOfstringanyType>");
		xml.append("<b:key>ColumnSet</b:key>");
		xml.append("<b:value i:type=\"a:ColumnSet\">");
		xml.append("<a:AllColumns>false</a:AllColumns>");
		xml.append("<a:Columns xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">");
		xml.append("<c:string>firstname</c:string>");
		xml.append("<c:string>lastname</c:string>");
		xml.append("</a:Columns>");
		xml.append("</b:value>");
		xml.append("</a:KeyValuePairOfstringanyType>");
		xml.append("</a:Parameters>");
		xml.append("<a:RequestId i:nil=\"true\" />");
		xml.append("<a:RequestName>Retrieve</a:RequestName>");
		xml.append("</request>");
		xml.append("</Execute>");
		xml.append("</s:Body>");

		Document xDoc = CrmExecuteSoap.ExecuteSoapRequest(authHeader,
				xml.toString(), url);
		if (xDoc == null)
			return null;

		String firstname = "";
		String lastname = "";

		NodeList nodes = xDoc
				.getElementsByTagName("b:KeyValuePairOfstringanyType");
		for (int i = 0; i < nodes.getLength(); i++) {
			if ((nodes.item(i).getFirstChild().getTextContent())
					.equals("firstname")) {
				firstname = nodes.item(i).getLastChild().getTextContent();
			}
			if ((nodes.item(i).getFirstChild().getTextContent())
					.equals("lastname")) {
				lastname = nodes.item(i).getLastChild().getTextContent();
			}
		}

		return firstname + " " + lastname;
	}
	
	public static String CrmGetOpportunityCount(CrmAuthenticationHeader authHeader,
			String id, String url) throws IOException, SAXException,
			ParserConfigurationException {
		String multipleRequest = "<RetrieveMultiple xmlns=\"http://schemas.microsoft.com/xrm/2007/Contracts/Services\"  "
				+ " xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
				" <query i:type=\"b:QueryExpression\" xmlns:b=\"http://schemas.microsoft.com/xrm/2006/Contracts\">" +
				" <b:ColumnSet>" +
		"       <b:AllColumns>true</b:AllColumns>" +
		"     </b:ColumnSet>" +
		"     <b:Criteria>" +
		"        <b:Conditions>" +
		"       </b:Conditions>" +
		"       <b:FilterOperator>And</b:FilterOperator>" +
		"       <b:Filters />" +
		" </b:Criteria>" +
//              " <b:Distinct>false</b:Distinct>" +
              " <b:EntityName>Opportunity</b:EntityName>" +
//              " <b:LinkEntities />" +
              " <b:Orders />" +
              " <b:PageInfo>" +
	              " <b:Count>1</b:Count>" +
	              " <b:PageNumber>1</b:PageNumber>" +	
	              " <b:PagingCookie i:nil=\"true\" />" +
	                		" <b:ReturnTotalRecordCount>true</b:ReturnTotalRecordCount>" +
              " </b:PageInfo>" +
              "     </query>" +
        " </RetrieveMultiple>";
		
		String requestMain  ="";
        requestMain += "    <Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">";
        requestMain += "      <request i:type=\"a:RetrieveMultipleRequest\" xmlns:a=\"http://schemas.microsoft.com/xrm/2011/Contracts\">";
        requestMain += "        <a:Parameters xmlns:b=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\">";
        requestMain += "          <a:KeyValuePairOfstringanyType>";
        requestMain += "            <b:key>Query</b:key>";
        requestMain += "            <b:value i:type=\"a:QueryExpression\">";
        requestMain += "              <a:ColumnSet>";
        requestMain += "                <a:AllColumns>true</a:AllColumns>";
//        requestMain += "                <a:Columns xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">";
//        requestMain += "                  <c:string>name</c:string>";
//        requestMain += "                </a:Columns>";
        requestMain += "              </a:ColumnSet>";
//        requestMain += "              <a:Criteria>";
//        requestMain += "                <a:Conditions />";
//        requestMain += "                <a:FilterOperator>And</a:FilterOperator>";
//        requestMain += "                <a:Filters />";
//        requestMain += "              </a:Criteria>";
        requestMain += "              <a:Distinct>false</a:Distinct>";
        requestMain += "              <a:EntityName>opportunity</a:EntityName>";
        requestMain += "              <a:Orders />";
        requestMain += "              <a:PageInfo>";
        requestMain += "                <a:Count>1</a:Count>";
        requestMain += "                <a:PageNumber>1</a:PageNumber>";
        requestMain += "                <a:PagingCookie i:nil=\"true\" />";
        requestMain += "                <a:ReturnTotalRecordCount>true</a:ReturnTotalRecordCount>";
        requestMain += "              </a:PageInfo>";
        requestMain += "              <a:NoLock>false</a:NoLock>";
        requestMain += "            </b:value>";
        requestMain += "          </a:KeyValuePairOfstringanyType>";
        requestMain += "        </a:Parameters>";
        requestMain += "        <a:RequestId i:nil=\"true\" />";
        requestMain += "        <a:RequestName>RetrieveMultiple</a:RequestName>";
        requestMain += "      </request>";
        requestMain += "    </Execute>";
		StringBuilder xml = new StringBuilder();
		xml.append("<s:Body>");
//		xml.append("<Execute  xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
		xml.append(requestMain);
//		xml.append("</Execute>");
		xml.append("</s:Body>");

		System.out.println("request " + xml.toString());
		Document xDoc = CrmExecuteSoap.ExecuteSoapRequest(authHeader,
				xml.toString(), url);
		
		if (xDoc == null)
			return null;

		String firstname = "";
		String lastname = "";
		System.out.println(" Output " + xDoc.toString());
//		NodeList nodes = xDoc
//				.getElementsByTagName("b:KeyValuePairOfstringanyType");
//		for (int i = 0; i < nodes.getLength(); i++) {
//			if ((nodes.item(i).getFirstChild().getTextContent())
//					.equals("firstname")) {
//				firstname = nodes.item(i).getLastChild().getTextContent();
//			}
//			if ((nodes.item(i).getFirstChild().getTextContent())
//					.equals("lastname")) {
//				lastname = nodes.item(i).getLastChild().getTextContent();
//			}
//		}

//		return firstname + " " + lastname;
		return "";
	}
}
