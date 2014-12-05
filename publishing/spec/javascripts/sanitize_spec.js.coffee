#= require jquery
#= require jquery.autosize.min
#= require application

describe "sanitizeQuotes", ->
  it "returns normal quotes 1", -> # Alt + ] produces an opening single curly quote ( ‘ )
    sanitizeQuotes("‘").should.equal("'")
  it "returns normal quotes 2", -> # Alt + Shift + ] produces a closing single curly quote ( ’ )
    sanitizeQuotes("’").should.equal("'")
  it "returns normal quotes 3", -> # Alt + [ produces an opening double curly quote ( “ )
    sanitizeQuotes("“").should.equal("\"")
  it "returns normal quotes 4", -> # Alt + Shift + [ produces a closing double curly quote ( ” )
    sanitizeQuotes("”").should.equal("\"")
  it "returns normal quotes 5", ->
    sanitizeQuotes("").should.equal("\"")


describe "sanitizeAmpersands", ->
  it "returns &amp; when text contains &", ->
    sanitizeAmpersands("BLA&BLA").should.equal("BLA&amp;BLA")
  it "doesnt replace other htmlenitities", ->
    sanitizeAmpersands("BLA&amp;BLA").should.equal("BLA&amp;BLA")
  it "doesnt replace other htmlenitities in capitals", ->
    sanitizeAmpersands("BLA&QUOte;BLA").should.equal("BLA&QUOte;BLA")
  it "doesnt replace other htmlenitities with #", ->
    sanitizeAmpersands("BLA&#2424;BLA").should.equal("BLA&#2424;BLA")
  it "works on bigger texts", ->
    txt = "test\n\twith&eacute;&eacute;n file with different &#22;&#1244; &#33; test met &amp;bla &amp;\n\t&auml;"
    sanitizeAmpersands(txt).should.equal(txt)
