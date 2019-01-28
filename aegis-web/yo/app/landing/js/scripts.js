$(document).ready(function () {

    /* STICKY NAVIGATION  */
    $('.js--section-features').waypoint(function (direction) {
        if (direction == "down") {
            $('nav').addClass('sticky');
        } else {
            $('nav').removeClass('sticky');
        }
    }, {
            offset: '80px'
        });

    /* NAVIGATION SCROLL */

    $(function () {
        $('a[href*="#"]:not([href="#"])').click(function () {
            if (location.pathname.replace(/^\//, '') == this.pathname.replace(/^\//, '') && location.hostname == this.hostname) {
                var target = $(this.hash);
                target = target.length ? target : $('[name=' + this.hash.slice(1) + ']');
                if (target.length) {
                    $('html, body').animate({
                        scrollTop: target.offset().top
                    }, 1000);

                    target.focus(); // Setting focus
                    if (target.is(":focus")) { // Checking if the target was focused
                        return false;
                    } else {
                        target.attr('tabindex', '-1'); // Adding tabindex for elements not focusable
                        target.focus(); // Setting focus
                    };

                    return false;
                }
            }
        });
    });

    /* ANIMATIONS ON SCROLL */

    $('.js--wp-1').waypoint(function (direction) {
        $('.js--wp-1').addClass('animated fadeIn');
    }, {
            offset: '50%'
        });


    $('.js--wp-2').waypoint(function (direction) {
        $('.js--wp-2').addClass('animated fadeInUp');
    }, {
            offset: '50%'
        });


    $('.js--wp-3').waypoint(function (direction) {
        $('.js--wp-3').addClass('animated fadeIn');
    }, {
            offset: '50%'
        });


    $('.js--wp-4').waypoint(function (direction) {
        $('.js--wp-4').addClass('animated pulse');
    }, {
            offset: '50%'
        });


    /* MOBILE NAVIGATION */

    $('.js--nav-icon').click(function () {
        var nav = $('.js--main-nav');
        
        nav.slideToggle(200);

    });
});