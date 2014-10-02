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

        obj.dd.on('click', function(event){
            $(this).toggleClass('active');
            return true;
        });
        /*
        obj.opts.on('click',function(){
            var opt = $(this);
            obj.val = opt.text();
            obj.index = opt.index();
            obj.placeholder.text(obj.val);
        }); */
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
/*
    $(document).click(function() {
        // all dropdowns
        $('.m-select-box').removeClass('active');
        });
*/
    });

