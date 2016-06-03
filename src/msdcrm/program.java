package msdcrm;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
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
	static String url;
	static String username;
	static String password;
	static CrmAuth auth;
	public static void main(String[] args) throws Exception {

		auth = new CrmAuth();

		url = args[0];
		username =args[1]; 
		password = args[2];
		
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
		CrmGetOpportunityIds(authHeader, "Audit", url, fld_list, "opportunity");
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
	
	public static Map CrmGetEntity(CrmAuthenticationHeader authHeader,
			String entity, String url, List<String> fld_list, String id) throws IOException, SAXException,
			ParserConfigurationException {

		StringBuilder xml = new StringBuilder();
		xml.append("<s:Body>");
		xml.append("<Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
		xml.append("<request i:type=\"a:RetrieveRequest\" xmlns:a=\"http://schemas.microsoft.com/xrm/2011/Contracts\">");
		xml.append("<a:Parameters xmlns:b=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\">");
		xml.append("<a:KeyValuePairOfstringanyType>");
		xml.append("<b:key>Target</b:key>");
		xml.append("<b:value i:type=\"a:EntityReference\">");
		xml.append("<a:Id>" + id + "</a:Id>");
		xml.append("<a:LogicalName>"+ entity +"</a:LogicalName>");
		xml.append("<a:Name i:nil=\"true\" />");
		xml.append("</b:value>");
		xml.append("</a:KeyValuePairOfstringanyType>");
		xml.append("<a:KeyValuePairOfstringanyType>");
		xml.append("<b:key>ColumnSet</b:key>");
		xml.append("<b:value i:type=\"a:ColumnSet\">");
//		if (fld_list == null || fld_list.size() == 0){
		xml.append("<a:AllColumns>true</a:AllColumns>");
//		} else {
//			xml.append("<a:Columns xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">");
//			for (String fld: fld_list){
//				xml.append("<c:string>firstname</c:string>");
//			}
//			xml.append("</a:Columns>");
//		}
		xml.append("</b:value>");
		xml.append("</a:KeyValuePairOfstringanyType>");
		xml.append("</a:Parameters>");
		xml.append("<a:RequestId i:nil=\"true\" />");
		xml.append("<a:RequestName>Retrieve</a:RequestName>");
		xml.append("</request>");
		xml.append("</Execute>");
		xml.append("</s:Body>");


//		System.out.println("request " + xml.toString());
		Document xDoc = CrmExecuteSoap.ExecuteSoapRequest(authHeader,
				xml.toString(), url);
		
		if (xDoc == null)
			return null;

		NodeList nodes = xDoc
				.getElementsByTagName("b:KeyValuePairOfstringanyType");
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 1; i < nodes.getLength(); i++) {
			String field = nodes.item(i).getFirstChild().getTextContent();
			String temp=null,value=null;
			if (nodes.item(i).getLastChild().hasChildNodes()){
				temp = nodes.item(i).getLastChild().getFirstChild().getTextContent();
				value = nodes.item(i).getLastChild().getLastChild().getTextContent();
			} else{
				value = nodes.item(i).getLastChild().getTextContent();
			}
			if (value == "" || temp == value){
				map.put(field, value.replace("\n", " ").replace("\t", " ").replace("\r", " "));
			} else {
//				System.out.println(i+field+value);
				String ref_entity = nodes.item(i).getLastChild().getLastChild().getPreviousSibling().getTextContent();
				map.put(field + "_" + ref_entity + "_id", temp);
				map.put(field+"_name", value);
			}
		}
//		return firstname + " " + lastname;
		return map;
	}

	private static void CrmGetOpportunityIds(CrmAuthenticationHeader authHeader, String entity, String url,
			List<String> fld_list, String entityname) {
		int batch = 500;
		int pg_num = 1;
		boolean more = true;
		List<String> master_id_list = new ArrayList<String>();
		Writer writer = null;
		Writer fwriter = null;
		Map<String, String> map= null;
		List<String> field_list = null;
		int cc = 0;
		try {
			while (more) {
				try {
					if (writer == null) {
					    writer = new BufferedWriter(new OutputStreamWriter(
					          new FileOutputStream("/home/santhosh/Desktop/"+entityname+"_ids.txt"), "utf-8"));
					}
					Result res = CrmGetids(authHeader, "Audit", url, fld_list, batch, pg_num, entityname, true);
					more = res.more;
					if (more) {
						System.out.println("Adding the pg_num" + pg_num);
						pg_num += 1;
					}
					System.out.println("id map keys" + res.id_map.keySet());
					List<String> opp_ids = res.id_map.get(entityname);
					master_id_list.addAll(opp_ids);
					if (opp_ids.size() !=0){
						if (fwriter == null) {
							map =CrmGetEntity(authHeader, entityname, url, fld_list, opp_ids.get(0));
							field_list = new ArrayList<String>(map.keySet());
						    fwriter = new BufferedWriter(new OutputStreamWriter(
						          new FileOutputStream("/home/santhosh/Desktop/"+entityname+".csv"), "utf-8"));
						 // Write Header to CSV
							StringUtils.join(field_list, "~");
							fwriter.write(StringUtils.join(field_list, "~")+"\n");
						}
						// get request all attributes entiry from entity
						for (String id: opp_ids){
							cc +=1;
							try{
								map =CrmGetEntity(authHeader, entityname, url, fld_list, id);
							} catch(Exception e){
								System.out.println("failed retrying again");
								map =CrmGetEntity(authHeader, entityname, url, fld_list, id);
							}
							System.out.println("Fetched record num:"+cc);
							each_record(fwriter, map, field_list);
							
						}
						authHeader = auth.GetHeaderOnPremise(username, password, url);
					}
				
				    for (String line:opp_ids) {
				    	writer.write(line + "\n");
				    }
					System.out.println(" master_id_list size " + master_id_list.size() );
//					if ( master_id_list.size() > 500) {
//						System.out.println(" master_id_list break" );
//						break;
//					}
//					System.out.println("size " + master_id_list.size());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					more = false;
				} 
			}
		} finally {
			try {writer.close(); fwriter.close();} catch (Exception ex) {/*ignore*/}
		}
	}
	public static void each_record(Writer fp, Map map, List<String> field_list) throws IOException{
		ArrayList<Map<String, String>> myArrList = new ArrayList<Map<String, String>>();
		myArrList.add(map);
		
		
		for (int i = 0; i < myArrList.size(); i++) {	
			String temp = "",value = "";
			for (String fld: field_list){
				value = myArrList.get(i).get(fld);
				if (value != null)
					temp += value.toString();
				if (field_list.indexOf(fld) != field_list.size()-1)
					temp+="~";
				
			}
			temp +="\n";
			fp.write(temp);
		}
		
	}
	public static void Crmidfromfile(CrmAuthenticationHeader authHeader, int lineno, String filepath, int pg_num) throws IOException, SAXException,
	ParserConfigurationException {
		
	}
	
	public static Result CrmGetids(CrmAuthenticationHeader authHeader,
			String entity, String url, List<String> fld_list, int cnt, int pg_num, String entityname, boolean id_only) throws IOException, SAXException,
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
        requestMain += "              <a:Criteria>";
        requestMain += "                <a:Conditions>";
        requestMain += "                 	<b:ConditionExpression>";
        requestMain += "                  		<b:AttributeName>createdon</b:AttributeName>";
        requestMain += "                  		<b:Operator>GreaterEqual</b:Operator>";
        requestMain += "                  		<b:Values xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">";
        requestMain += "                    		<c:anyType xmlns:d=\"http://www.w3.org/2001/XMLSchema\" i:type=\"d:dateTime\">2014-01-01T00:00:00Z</c:anyType>";
        requestMain += "                  		</b:Values>";
        requestMain += "                 	</b:ConditionExpression>";
        requestMain += "                 </a:Conditions>";
        requestMain += "                <a:FilterOperator>And</a:FilterOperator>";
        requestMain += "                <a:Filters />";
        requestMain += "              </a:Criteria>";
        requestMain += "              <a:Distinct>false</a:Distinct>";
        requestMain += "              <a:EntityName>" + entityname + "</a:EntityName>";
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
//		xml.append("<Execute xmlns=\"http://schemas.microsoft.com/xrm/2011/Contracts/Services\"			xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">			<request i:type=\"a:RetrieveMultipleRequest\"				xmlns:a=\"http://schemas.microsoft.com/xrm/2011/Contracts\">				<a:Parameters					xmlns:b=\"http://schemas.datacontract.org/2004/07/System.Collections.Generic\">					<a:KeyValuePairOfstringanyType>						<b:key>Query</b:key>						<b:value i:type=\"a:QueryExpression\">							<a:ColumnSet>								<a:AllColumns>true</a:AllColumns>							</a:ColumnSet>							<a:Criteria>								<a:Conditions>								</a:Conditions>								<a:FilterOperator>And</a:FilterOperator>								<a:Filters />							</a:Criteria>							<a:Distinct>false</a:Distinct>							<a:EntityName>opportunity</a:EntityName>							<a:LinkEntities />							<a:Orders />							<a:PageInfo>								<a:Count>1</a:Count>								<a:PageNumber>1</a:PageNumber>								<a:PagingCookie i:nil=\"true\" />								<a:ReturnTotalRecordCount>true</a:ReturnTotalRecordCount>							</a:PageInfo>							<a:NoLock>false</a:NoLock>						</b:value>					</a:KeyValuePairOfstringanyType>				</a:Parameters>				<a:RequestId i:nil=\"true\" />				<a:RequestName>RetrieveMultiple</a:RequestName>			</request>		</Execute>");
//		xml.append("</Execute>");
		xml.append("</s:Body>");
//		System.out.println(xml.toString());
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
				if (id_only)
					if (childNodes.item(j).getNodeName() == "b:Id") {
						id = childNodes.item(j).getTextContent();
					}
					if (childNodes.item(j).getNodeName() == "b:LogicalName") {
						name = childNodes.item(j).getTextContent();
				}
//				System.out.println(" nodes " + childNodes.item(j).getNodeName());
			}
			if (id_only)
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
