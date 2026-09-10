/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.tci.oidc.api.simple;

import software.xdev.tci.oidc.api.HttpClientBasedOIDCServerMockApi;


public class SimpleOIDCServerMockApi extends HttpClientBasedOIDCServerMockApi
{
	public SimpleOIDCServerMockApi(final String externalHttpBaseEndPoint)
	{
		super(externalHttpBaseEndPoint);
	}
	
	@Override
	public String addUser(
		final String email,
		final String name,
		final String pw)
	{
		final String subjectId = this.nextSubjectId();
		this.apiAddUser(this.createDefaultBodyForAddUser(subjectId, email, name, pw));
		return subjectId;
	}
	
	public void replaceUser(
		final String subjectId,
		final String email,
		final String name,
		final String pw)
	{
		this.apiReplaceUser(this.createDefaultBodyForAddUser(subjectId, email, name, pw));
	}
	
	protected String createDefaultBodyForAddUser(
		final String subjectId,
		final String email,
		final String name,
		final String pw)
	{
		return """
			{
			  "SubjectId":"%s",
			  "Username":"%s",
			  "Password":"%s",
			  "Claims": [
			    {
			      "Type": "name",
			      "Value": "%s",
			      "ValueType": "string"
			    },
			    {
			      "Type": "email",
			      "Value": "%s",
			      "ValueType": "string"
			    }
			  ]
			}
			""".formatted(
			subjectId,
			email,
			pw,
			name,
			email
		);
	}
}
