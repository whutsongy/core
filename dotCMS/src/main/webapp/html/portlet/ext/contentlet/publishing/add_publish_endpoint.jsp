<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher" %>

<%
	String identifier = request.getParameter("id");
	String environmentId = request.getParameter("environmentId");
	String isSender = request.getParameter("isSender");

	PublishingEndPointAPI peAPI = APILocator.getPublisherEndPointAPI();
	PublishingEndPoint currentEndpoint = peAPI.findEndPointById(identifier);

	Environment currentEnvironment = APILocator.getEnvironmentAPI().findEnvironmentById(environmentId);

	if(currentEndpoint ==null){
		currentEndpoint = new PublishingEndPoint();
		currentEndpoint.setEnabled(true);
		currentEndpoint.setPort("");
		currentEndpoint.setProtocol("");
		currentEndpoint.setSending(false);

		if(currentEnvironment!=null)
			currentEndpoint.setGroupId(currentEnvironment.getId());

	}

	List<String> groups = peAPI.findSendGroups();
%>

<script type="text/javascript">

	// if it is aws s3 selected, the password should be displayed.
	var currentProtocol = "<%= currentEndpoint.getProtocol() %>";

	require(["dojo/parser", "dijit/form/SimpleTextarea"]);
	function saveEndpoint(){

		var form = dijit.byId("formSaveEndpoint");

		dijit.byId("serverName").setAttribute('required',true);
		dijit.byId("address").setAttribute('required',true);

		if(dojo.byId("sending").value=='false'){
			dijit.byId("port").setAttribute('required',true);
		}
		else{
			dijit.byId("port").setAttribute('required',false);
		}
		
		if (form.validate()) {

			dijit.byId("save").setAttribute('disabled',true);

			var xhrArgs = {
				url: "/DotAjaxDirector/com.dotcms.publisher.endpoint.ajax.PublishingEndpointAjaxAction/cmd/addEndpoint",
				form: dojo.byId("formSaveEndpoint"),
				handleAs: "text",
				load: function(data){
					if(data.indexOf("FAILURE") > -1){
						dijit.byId("save").setAttribute('disabled',false);

						alert(data);
					}
					else{
						<% if (UtilMethods.isSet(environmentId)) { %>
						backToEnvironmentList(true);
						<% } else {%>
						backToEndpointsList();
						<%}%>
					}
				},
				error: function(error){
					dijit.byId("save").setAttribute('disabled',false);
					
					alert(error);
				}
			}

			var deferred = dojo.xhrPost(xhrArgs);
		}

	}


	function enableSave(){
		var servernamevalue = dojo.byId("serverName").getValue();

		if(servernamevalue && servernamevalue.length > 0){
			dijit.byId("save").setAttribute('disabled',false);
		}else{

			dijit.byId("save").setAttribute('disabled',true);
		}

	}

	function toggleServerType(sending){

		if(sending=='false'){
			dojo.style("addressFromSpan", "display", "none");
			dojo.style("addressToSpan", "display", "");
			
			dojo.style("sendGroupRow", "display", "flex");
		}
		else{
			dojo.style("protocolRow", "display", "none");
			dojo.style("portSpan", "display", "none");
			dojo.style("addressRow", "display", "");
			dojo.style("authPropertiesRow", "display", "");

		}
	}

    function setAddressRow(changedType) {

        if (currentProtocol === "awss3" && isPlatformLicenseLevel()) {

            if (changedType) {
                dijit.byId("address").set("value", "s3.aws.amazon.com");
                dijit.byId("port").set("value", "80");

                dojo.byId("addressRow").hide();
            }
        } else {
            if (changedType) {
                if(dijit.byId("address")){
                    dijit.byId("address").set("value", "");
                }
                if(dojo.byId("addressRow")){
                    dojo.byId("addressRow").show();
                }
            }
        }
    }

	function setAuthPropertiesRow(changedType) {

		if (currentProtocol === "awss3" && isPlatformLicenseLevel()) {

			dojo.style("authKeyHttpSpan", "display", "none");
			dojo.style("authKeyAwsS3Span", "display", "");

			if (dijit.byId("authKey").value.trim().length == 0 || changedType) {

				dijit.byId("authKey").set("value",
					"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_TOKEN%>=myToken\n" +
					"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_SECRET%>=mySecret\n" +
					"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID%>=dotcms-bucket-{hostname}-{languageIso}\n" +
					"<%=AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_REGION%>=us-west-2"
				);
			}
		} else {

			dojo.style("authKeyHttpSpan", "display", "");
			dojo.style("authKeyAwsS3Span", "display", "none");

            if (changedType || !isPlatformLicenseLevel()) {
                if(dijit.byId("authKey")){
                    dijit.byId("authKey").set("value", "");
                }
            }
		}

		if (changedType) {
			dojo.byId("authPropertiesRow").show();
		}
	}


	function onChangeProtocolTypeSelectCheck() {
		currentProtocol = dijit.byId("protocol").value;

		setAddressRow(true);		
		setAuthPropertiesRow(true);
	}

	function isPlatformLicenseLevel() {
		<% if(LicenseUtil.getLevel()>400){ %>
			return true;
		<%} else { %>
			return false;
		<%} %>
	}

	dojo.ready( function(){

		<% if( ! com.dotmarketing.util.UtilMethods.isSet( currentEndpoint.getProtocol() ) ) { %>
			dojo.byId("addressRow").hide();
			dojo.byId("authPropertiesRow").hide();
		<% } %>

		if (currentProtocol === "awss3" && isPlatformLicenseLevel()) {
			dojo.byId("addressRow").hide();				
		}

		setAddressRow(false);
		setAuthPropertiesRow(false);

		toggleServerType('<%=isSender%>');
	});

</script>


<div style="margin:auto;">
	<div dojoType="dijit.form.Form"  name="formSaveEndpoint"  id="formSaveEndpoint" onsubmit="return false;">
		<input type="hidden" name="sending" id="sending" value="<%=isSender%>">
		<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getId())) %>">
		<div class="form-horizontal">
			<%if(currentEnvironment!=null) { %>
				<dl id="sendGroupRow">
					<dt>
						<%= LanguageUtil.get(pageContext, "publisher_Environment") %>:
					</dt>
					<dd>
						<%=currentEnvironment.getName() %>
						<input type="hidden" id="environmentId" name="environmentId" value="<%=currentEnvironment.getId() %>">
					</dd>
				</dl>
			<%} %>
			<dl>
				<dt>
					<label for="serverName"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %>:</label>
				</dt>
				<dd>
					<input type="text" dojoType="dijit.form.ValidationTextBox"
						   name="serverName"
						   id="serverName"
						   style="width:300px;"
						   value="<%=UtilMethods.webifyString(String.valueOf(currentEndpoint.getServerName())) %>"
						   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_ServerName_Prompt_Message") %>"
					/>
				</dd>
			</dl>

			<dl id="protocolRow">
				<dt>
					<label form="protocol"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Type") %>:</label>
				</dt>
				<dd>
					<select dojoType="dijit.form.Select" name="protocol" id="protocol" style="width:100px;" onchange="onChangeProtocolTypeSelectCheck();">
						<% if( ! com.dotmarketing.util.UtilMethods.isSet( currentEndpoint.getProtocol() ) ) { %>
							<option disabled="disabled" selected="selected" value=""><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_placeholder") %></option>
						<%} %>
						<option value="http" <%=("http".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_http") %></option>
						<option value="https" <%=("https".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_https") %></option>
						<%if(LicenseUtil.getLevel()>400){ %>
							<option value="awss3" <%=("awss3".equals(currentEndpoint.getProtocol())) ? "selected=true" : "" %>><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_awss3") %></option>
						<%}else{ %>
							<option value="" disabled=true><%= LanguageUtil.get(pageContext, "publisher_Endpoint_type_awss3_requires_platform_license") %></option>
						<%} %>
					</select>
				</dd>
			</dl>

			<dl id="addressRow">
				<dt>
					<span id="addressToSpan">
						<label for="address"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_To") %>:</label>
					</span>
					<span id="addressFromSpan" style="display:none;">
						<label for="address"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Address_From") %>:</label>
					</span>
				</dt>
				<dd>
					<div class="inline-form">
						<input type="text" dojoType="dijit.form.ValidationTextBox"
							   name="address"
							   id="address"
							   style="width:300px"
							   value="<%=UtilMethods.webifyString(currentEndpoint.getAddress()) %>"
							   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Address_Prompt_Message") %>"
						/>
						<span id="portSpan">
							<label for="port"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Port") %>:</label>
							<input type="text" dojoType="dijit.form.ValidationTextBox"
								   name="port" id="port" style="width:60px"
								   value="<%=UtilMethods.webifyString(currentEndpoint.getPort()) %>"
								   promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Prompt_Message") %>" regExp="^[0-9]+$" invalidMessage="<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Validation_Port_Invalid_Message") %>" />
						</span>
					</div>
					<div id="addressHelpText" class="hint-text">e.g. 10.0.1.10 or server2.myhost.com</div>
				</dd>
			</dl>

			<dl id="authPropertiesRow">
				<dt>
					<span id="authKeyHttpSpan">
						<label for="authKey"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key_type_http") %>:</label>
					</span>
					<span id="authKeyAwsS3Span" style="display:none;">
						<label for="authKey"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Auth_key_type_awss3") %>:</label>
					</span>
				</dt>
				<dd>
					<textarea dojoType="dijit.form.SimpleTextarea" name="authKey" id="authKey" style="width:400px;height:105px;"><%=( currentEndpoint.getAuthKey() != null && currentEndpoint.getAuthKey().length() > 0) ? PublicEncryptionFactory.decryptString( currentEndpoint.getAuthKey().toString())  : "" %></textarea>
				</dd>
			</dl>

			<dl>
				<dt>
					<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Enabled") %>:
				</dt>
				<dd>
					<input dojoType="dijit.form.CheckBox" type="checkbox" name="enabled" <%=(currentEndpoint.isEnabled()) ? "checked=true" : "" %> />
				</dd>
			</dl>
		</div>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" type="submit" id="save" onclick="saveEndpoint()"><%= LanguageUtil.get(pageContext, "Save") %></button>
			<button dojoType="dijit.form.Button" onClick="backToEndpointsList(true)" id="closeSave" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
		</div>
	</div>
</div>