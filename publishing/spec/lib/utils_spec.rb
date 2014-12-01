require 'rails_helper'


describe "pretty_debug" do
  it "should log the error with a date" do
    text = 'foutmelding'
    expect(Rails.logger).to receive(:debug).exactly(7).times.with(any_args())
    pretty_debug(text)
  end
  it "should log the error to another log" do
    text = 'foutmelding'
    expect(Rails.logger).to receive(:notice).exactly(7).times.with(any_args())
    pretty_debug(text, :notice)
  end
  it "should log the error with a delta time" do
    text = 'foutmelding'
    pretty_debug(text, :debug, true)
    expect(Rails.logger).to receive(:debug).exactly(3).times.with(any_args())
    pretty_debug(text)
  end
end

describe "render_latex" do
  before do
    @url = "#{ StudyflowPublishing::Application.config.latex_server }/"
  end

  def content_tag(tag,something,something_else)
    "#{something}"
  end

  describe "for editing" do

  it "should render a latex formula in svg" do
    formula_inline = 'x'
    parsed_formula_inline = %(<svg xmlns:xlink="http://www.w3.org/1999/xlink" style="width: 1.286ex; height: 1.143ex; vertical-align: -0.143ex; margin-top: 1px; margin-right: 0px; margin-bottom: 1px; margin-left: 0px; position: static; " viewBox="0 -468.258123255155 577 505.51624651031" xmlns="http://www.w3.org/2000/svg"><defs id="MathJax_SVG_glyphs"><path id="MJMATHI-78" stroke-width="10" d="M52 289Q59 331 106 386T222 442Q257 442 286 424T329 379Q371 442 430 442Q467 442 494 420T522 361Q522 332 508 314T481 292T458 288Q439 288 427 299T415 328Q415 374 465 391Q454 404 425 404Q412 404 406 402Q368 386 350 336Q290 115 290 78Q290 50 306 38T341 26Q378 26 414 59T463 140Q466 150 469 151T485 153H489Q504 153 504 145Q504 144 502 134Q486 77 440 33T333 -11Q263 -11 227 52Q186 -10 133 -10H127Q78 -10 57 16T35 71Q35 103 54 123T99 143Q142 143 142 101Q142 81 130 66T107 46T94 41L91 40Q91 39 97 36T113 29T132 26Q168 26 194 71Q203 87 217 139T245 247T261 313Q266 340 266 352Q266 380 251 392T217 404Q177 404 142 372T93 290Q91 281 88 280T72 278H58Q52 284 52 289Z"></path></defs><g stroke="black" fill="black" stroke-width="0" transform="matrix(1 0 0 -1 0 0)"><use href="#MJMATHI-78" xlink:href="#MJMATHI-78"></use></g></svg>)
    text_inline = "This is a text and <math>#{formula_inline}</math>"

    response_object = Net::HTTPOK.new('1.1', 200, 'OK')
    allow(response_object).to receive(:body)
    parsed_response = lambda { parsed_formula_inline }
    mocked_response = HTTParty::Response.new({},response_object,parsed_response)
    expect(HTTParty).to receive(:post).with(@url, body: formula_inline).and_return(mocked_response)
    expect(render_latex_for_editing(text_inline)).to eq(%(This is a text and <span class="latex">#{parsed_formula_inline}</span>))
  end

    it "should show a big notice when LaTeX cannot be rendered" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      parsing_error = "MathJax error"
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body)
      parsed_response = lambda { parsing_error }
      mocked_response = HTTParty::Response.new({}, response_object, parsed_response)
      expect(HTTParty).to receive(:post).with(@url, body: wrong_formula).and_return(mocked_response)
      expect(render_latex_for_editing(text_with_wrong_formula)).to eq(%(This text has a wrong formula here: <div class="alert alert-danger">'#{wrong_formula}' is not valid LaTeX</div>))
    end

    it "should show a big notice when a formula is not correct" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      parsing_error = %(<div class="alert alert-danger">'{' is not valid LaTeX</div>)
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body)
      parsed_response = lambda { parsing_error }
      mocked_response = HTTParty::Response.new({}, response_object, parsed_response)
      expect(HTTParty).to receive(:post).with(@url, body: wrong_formula).and_return(mocked_response)
      expect(render_latex_for_editing(text_with_wrong_formula)).to eq(%(This text has a wrong formula here: <span class="latex"><div class="alert alert-danger">'#{wrong_formula}' is not valid LaTeX</div></span>))
    end

    it "should display a notice when the connection with the LaTeX rendering engine is refused" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      error = "Connection to LaTeX renderer refused"
      expect(render_latex_for_editing(text_with_wrong_formula)).to eq(%(<div class="alert alert-danger">Error with LaTeX rendering: #{error}</div>))
    end

    it "should display a notice when connection with the LaTeX rendering engine timeouts" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      parsing_error = "MathJax error"
      expect(HTTParty).to receive(:post).with(@url, body: wrong_formula).and_raise(Net::ReadTimeout)
      error = "Connection to LaTeX renderer had a timeout"
      expect(render_latex_for_editing(text_with_wrong_formula)).to eq(%(<div class="alert alert-danger">Error with LaTeX rendering: #{error}</div>))

    end

  end

  describe "for publishing" do

  it "should render a latex formula in svg" do
    formula_inline = 'x'
    parsed_formula_inline = %(<svg xmlns:xlink="http://www.w3.org/1999/xlink" style="width: 1.286ex; height: 1.143ex; vertical-align: -0.143ex; margin-top: 1px; margin-right: 0px; margin-bottom: 1px; margin-left: 0px; position: static; " viewBox="0 -468.258123255155 577 505.51624651031" xmlns="http://www.w3.org/2000/svg"><defs id="MathJax_SVG_glyphs"><path id="MJMATHI-78" stroke-width="10" d="M52 289Q59 331 106 386T222 442Q257 442 286 424T329 379Q371 442 430 442Q467 442 494 420T522 361Q522 332 508 314T481 292T458 288Q439 288 427 299T415 328Q415 374 465 391Q454 404 425 404Q412 404 406 402Q368 386 350 336Q290 115 290 78Q290 50 306 38T341 26Q378 26 414 59T463 140Q466 150 469 151T485 153H489Q504 153 504 145Q504 144 502 134Q486 77 440 33T333 -11Q263 -11 227 52Q186 -10 133 -10H127Q78 -10 57 16T35 71Q35 103 54 123T99 143Q142 143 142 101Q142 81 130 66T107 46T94 41L91 40Q91 39 97 36T113 29T132 26Q168 26 194 71Q203 87 217 139T245 247T261 313Q266 340 266 352Q266 380 251 392T217 404Q177 404 142 372T93 290Q91 281 88 280T72 278H58Q52 284 52 289Z"></path></defs><g stroke="black" fill="black" stroke-width="0" transform="matrix(1 0 0 -1 0 0)"><use href="#MJMATHI-78" xlink:href="#MJMATHI-78"></use></g></svg>)
    text_inline = "This is a text and <math>#{formula_inline}</math>"

    response_object = Net::HTTPOK.new('1.1', 200, 'OK')
    allow(response_object).to receive(:body)
    parsed_response = lambda { parsed_formula_inline }
    mocked_response = HTTParty::Response.new({},response_object,parsed_response)
    expect(HTTParty).to receive(:post).with(@url, body: formula_inline).and_return(mocked_response)
    expect(render_latex_for_publishing(text_inline)).to eq(%(This is a text and <span class="latex">#{parsed_formula_inline}</span>))
  end

    it "should throw an exception when LaTeX cannot be rendered" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      parsing_error = "MathJax error"
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body)
      parsed_response = lambda { parsing_error }
      mocked_response = HTTParty::Response.new({}, response_object, parsed_response)
      expect(HTTParty).to receive(:post).with(@url, body: wrong_formula).and_return(mocked_response)
      expect{render_latex_for_publishing(text_with_wrong_formula,"test")}.to raise_error(ArgumentError, /MathJax Error in 'test'/)
    end

    it "should throw an exception when the connection with the LaTeX rendering engine is refused" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      expect{render_latex_for_publishing(text_with_wrong_formula)}.to raise_error(ArgumentError, /Connection to LaTeX renderer refused/)
    end

    it "should throw an exception when connection with the LaTeX rendering engine timeouts" do
      wrong_formula = "{"
      text_with_wrong_formula = "This text has a wrong formula here: <math>#{wrong_formula}</math>"
      parsing_error = "MathJax error"
      expect(HTTParty).to receive(:post).with(@url, body: wrong_formula).and_raise(Net::ReadTimeout)
      expect{render_latex_for_publishing(text_with_wrong_formula,"test")}.to raise_error(ArgumentError, /Connection to LaTeX renderer had a timeout/)
    end


  end


  describe "preparse_images" do
    it "should not alter text" do
      parsed = preparse_images("test")
      expect(parsed).to eq "test"
    end

    it "should throw an exception on missing image dimensions" do
      src = %(<img src="https://assets.studyflow.nl/bg.jpg">)
      expect{preparse_images(src, "question")}.to raise_error(ArgumentError, /not found in question!/)
    end

    it "should replace html tag with image + dimensions" do
      Image.create(path: "/bg.jpg", status: "checked", width: 100, height: 200)
      parsed = preparse_images(%(<img src="https://assets.studyflow.nl/bg.jpg">))
      expect(parsed).to eq %(<img src="https://assets.studyflow.nl/bg.jpg" data-width="100" data-height="200">)
    end

  end

end
