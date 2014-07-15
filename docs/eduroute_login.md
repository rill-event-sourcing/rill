# EduRoute login flow

1. curl 'http://www.eduroute.nl//content.php/9789491682001\?LeerlingID\=test1\&SchoolID\=123456'
   <HTML><HEAD><TITLE>Eduroute</TITLE>
   <script language="JavaScript">top.location="http://www.eduroute.nl/redirect.php?session=e42527f56ded906c4f1760995595d99c";</script>
   </HEAD><BODY></BODY></HTML>

2. curl 'http://www.eduroute.nl/redirect.php?session=e42527f56ded906c4f1760995595d99c'
   <HTML><HEAD><TITLE>Eduroute</TITLE>
   <script language="JavaScript">top.location="https://www.studyflow.nl/students/sign_in?EAN=9789491682001&edurouteSessieID=e42527f56ded906c4f1760995595d99c&signature=3b948648dd8e21557bd0c7624f1bfab0";</script>
   </HEAD><BODY></BODY></HTML>

3. get request to: https://www.studyflow.nl/students/sign_in
    ?EAN=XXX
    &edurouteSessieID=XXX
    &signature=XXX

4. check MD5(session_id + pre shared key) == signature

5. @client = Savon.client(wsdl: 'http://www.eduroute.nl/soap/uitgever/uitgeverAPI.php?wsdl')

@response = @client.call(:sessie_login, :message => {:LeverancierCode => 'studyflow', :ControleCode => 'qW3#f65S', :SessieID => 'e42527f56ded906c4f1760995595d99c'})

body = @response.body[:sessie_login_response]
   {
   :res_param => "687740",
   :res_string => "{
    \"ID\":\"687740\",\"voornaam\":\"t?st1\",\"tussenvoegsel\":\"\",\"achternaam\":\"Persoon\",\"geslacht\":\"M\",\"assunr\":\"44544\",\"brin\":\"16FP00\",\"klas\":\"HAVO1\",
    \"studentnummer\":\"SNR1\",\"ELOIdentificatie\":\"\",\"ELO\":\"distributeurportaal\",\"ELOURL\":\"\"}"}


get to URL
    check MD5 of (session_id + PSK == signature) || Throw AuthException
    call to API and get student data || Throw AuthException
    unless (student.find_by_eduroute_id)
        mk student with data from Eduroute
    end
    login student

