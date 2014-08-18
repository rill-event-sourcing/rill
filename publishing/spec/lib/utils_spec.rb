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
    @url = "http://localhost:16000/"
  end

  def content_tag(tag,something,something_else)
    "#{something}"
  end

  it "should render a latex formula in svg" do
    formula_inline = 'x'
    parsed_formula_inline = %(<svg xmlns:xlink="http://www.w3.org/1999/xlink" style="width: 1.286ex; height: 1.143ex; vertical-align: -0.143ex; margin-top: 1px; margin-right: 0px; margin-bottom: 1px; margin-left: 0px; position: static; " viewBox="0 -468.258123255155 577 505.51624651031" xmlns="http://www.w3.org/2000/svg"><defs id="MathJax_SVG_glyphs"><path id="MJMATHI-78" stroke-width="10" d="M52 289Q59 331 106 386T222 442Q257 442 286 424T329 379Q371 442 430 442Q467 442 494 420T522 361Q522 332 508 314T481 292T458 288Q439 288 427 299T415 328Q415 374 465 391Q454 404 425 404Q412 404 406 402Q368 386 350 336Q290 115 290 78Q290 50 306 38T341 26Q378 26 414 59T463 140Q466 150 469 151T485 153H489Q504 153 504 145Q504 144 502 134Q486 77 440 33T333 -11Q263 -11 227 52Q186 -10 133 -10H127Q78 -10 57 16T35 71Q35 103 54 123T99 143Q142 143 142 101Q142 81 130 66T107 46T94 41L91 40Q91 39 97 36T113 29T132 26Q168 26 194 71Q203 87 217 139T245 247T261 313Q266 340 266 352Q266 380 251 392T217 404Q177 404 142 372T93 290Q91 281 88 280T72 278H58Q52 284 52 289Z"></path></defs><g stroke="black" fill="black" stroke-width="0" transform="matrix(1 0 0 -1 0 0)"><use href="#MJMATHI-78" xlink:href="#MJMATHI-78"></use></g></svg>)
    text_inline = "This is a text and <math>#{formula_inline}</math>"

    response_object = Net::HTTPOK.new('1.1', 200, 'OK')
    allow(response_object).to receive(:body)
    parsed_response = lambda { parsed_formula_inline }
    mocked_response = HTTParty::Response.new({},response_object,parsed_response)
    expect(HTTParty).to receive(:post).with(@url, body: formula_inline).and_return(mocked_response)
    expect(render_latex(text_inline)).to eq(%(This is a text and <span class="latex">#{parsed_formula_inline}</span>))
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
    expect(render_latex(text_with_wrong_formula)).to eq(%(This text has a wrong formula here: <div class="alert alert-danger">'#{wrong_formula}' is not valid LaTeX</div>))
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
    expect(render_latex(text_with_wrong_formula)).to eq(%(This text has a wrong formula here: <span class="latex"><div class="alert alert-danger">'#{wrong_formula}' is not valid LaTeX</div></span>))
  end

end
