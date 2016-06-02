package msdcrm;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class program {
	
	public static class Result {
		Map<String, List<String>> id_map;
		Boolean more;
		
		public Result(Map<String, List<String>> id_map2, Boolean more2) {
			this.id_map = id_map2;
			this.more = more2;
		}
	}
	
	
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
		List<String> fld_list = Arrays.asList("Id");
//		CrmGetOpportunityCount(authHeader, "Audit", url, fld_list);
		CrmGetOpportunityIds(authHeader, "Audit", url, fld_list);
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
			String entity, String url, List<String> fld_list) throws IOException, SAXException,
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
              " <b:EntityName>" + entity + "</b:EntityName>" +
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
        if (fld_list == null || fld_list.size() == 0) {
        	requestMain += "                <a:AllColumns>true</a:AllColumns>";
        } else{
//        	requestMain += "                <a:AllColumns>false</a:AllColumns>";
        	for (String fld : fld_list) {
//        		requestMain += "                <a:Columns  xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">";
                requestMain += "                  <a:string>" + fld +"</a:string>";
//                requestMain += "                </a:Columns>";
        	}
        }
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

	private static void CrmGetOpportunityIds(CrmAuthenticationHeader authHeader, String string, String url,
			List<String> fld_list) {
		int batch = 100;
		int pg_num = 1;
		boolean more = true;
		List<String> master_id_list = new ArrayList<String>();
		Writer writer = null;
		try {
			while (more) {
				try {
					if (writer == null) {
					    writer = new BufferedWriter(new OutputStreamWriter(
					          new FileOutputStream("opportunity.txt"), "utf-8"));
					}
					Result res = CrmGetOpportunityIds(authHeader, "Audit", url, fld_list, batch, pg_num);
					if (res.more) {
						System.out.println("Adding the pg_num" + pg_num);
						pg_num += 1;
					}
					System.out.println("id map keys" + res.id_map.keySet());
					List<String> opp_ids = res.id_map.get("opportunity");
					master_id_list.addAll(opp_ids);
				
				    for (String line:opp_ids) {
				    	writer.write(line + "\n");
				    }
					System.out.println(" master_id_list size " + master_id_list.size() );
					if ( master_id_list.size() > 500) {
						System.out.println(" master_id_list break" );
						break;
					}
					System.out.println("size " + master_id_list.size());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					more = false;
				} 
			}
		} finally {
			try {writer.close();} catch (Exception ex) {/*ignore*/}
		}
	}
	
	public static Result CrmGetOpportunityIds(CrmAuthenticationHeader authHeader,
			String entity, String url, List<String> fld_list, int cnt, int pg_num) throws IOException, SAXException,
			ParserConfigurationException {
		
		String requestMain  ="";
        requestMain += "    <Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">";
        requestMain += "      <request i:type=\"a:RetrieveMultipleRequest\" xmlns:a=\"http://schemas.microsoft.com/xrm/2011/Contracts\">";
        requestMain += "        <a:Parameters xmlns:b=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\">";
        requestMain += "          <a:KeyValuePairOfstringanyType>";
        requestMain += "            <b:key>Query</b:key>";
        requestMain += "            <b:value i:type=\"a:QueryExpression\">";
        requestMain += "              <a:ColumnSet>";
        if (fld_list == null || fld_list.size() == 0) {
        	requestMain += "                <a:AllColumns>true</a:AllColumns>";
        } else{
        	for (String fld : fld_list) {
                requestMain += "                  <a:string>" + fld +"</a:string>";
        	}
        }
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
        requestMain += "                <a:Count>"+ cnt+ "</a:Count>";
        requestMain += "                <a:PageNumber>" + pg_num + "</a:PageNumber>";
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

//		System.out.println("request " + xml.toString());
		Document xDoc = CrmExecuteSoap.ExecuteSoapRequest(authHeader,
				xml.toString(), url);
		
		if (xDoc == null)
			return null;

//		System.out.println(" Output " + xDoc.toString());
		NodeList nodes = xDoc.getElementsByTagName("b:Entity");
		Map<String, List<String>>id_map = new HashMap<String, List<String>>();
		System.out.println(" nodes.getLength(); " +  nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i++) {
//			System.out.println(" Entity nodes  " + nodes.item(i).getNodeName());
			NodeList childNodes = nodes.item(i).getChildNodes();
			String id = null;
			String name = null;
			for (int j = 0; j < childNodes.getLength(); j++) {
//				System.out.println("childNodes.getLength() " + childNodes.getLength());
				if (childNodes.item(j).getNodeName() == "b:Id") {
					id = childNodes.item(j).getTextContent();
				}
				if (childNodes.item(j).getNodeName() == "b:LogicalName") {
					name = childNodes.item(j).getTextContent();
				}
//				System.out.println(" nodes " + childNodes.item(j).getNodeName());
			}
			if ( id != null && name != null) {
				if (id_map.get(name) == null ) {
					List<String> id_list = new ArrayList<>();
					id_map.put(name, id_list);
				}
				id_map.get(name).add(id);
			}
		}
		NodeList morerecord = xDoc.getElementsByTagName("b:MoreRecords");
		Boolean more = Boolean.FALSE;
		for (int k=0; k< morerecord.getLength(); k++){
			more = Boolean.valueOf(morerecord.item(k).getTextContent());
		}
		return new Result(id_map, more);
	}
}
