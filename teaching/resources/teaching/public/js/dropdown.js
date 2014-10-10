function DropDown(el) {
    this.dd = el;
    this.placeholder = this.dd.children('span');
    this.opts = this.dd.find('ul.dropdown > li');
    this.val = '';
    this.index = -1;
    this.initEvents();
}
DropDown.prototype = {
    initEvents : function() {
        var obj = this;
        var i = 0;

        obj.dd.on('click', function(event){
            $(this).toggleClass('active');
            return true;
        });
    },
    getValue : function() {
        return this.val;
    },
    getIndex : function() {
        return this.index;
    }
}

$(function() {

    var dd1 = new DropDown( $('#dropdown-classes') );
    var dd2 = new DropDown( $('#dropdown-meijerink') );

    $(document).on('click', function(event) {

        if (!$(event.target).closest('#dropdown-classes').length) {
            $('#dropdown-classes').removeClass('active');
        }

        if (!$(event.target).closest('#dropdown-meijerink').length) {
            $('#dropdown-meijerink').removeClass('active');
        }
    });
});
