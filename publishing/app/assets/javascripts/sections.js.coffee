# Place all the behaviors and hooks related to the matching controller here.
# All this logic will automatically be available in application.js.
# You can use CoffeeScript in this file: http://coffeescript.org/

$ ->
  $('#subsection-tabs a').bind 'click', (event, data) ->
    url = $(event.target).data('url')
    $("#subsection-list").load(url);
