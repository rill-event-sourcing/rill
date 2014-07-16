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
