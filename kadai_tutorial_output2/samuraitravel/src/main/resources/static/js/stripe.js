const stripe = Stripe('pk_test_51RrwaKCnGHO85Cf91kfiGolJhKDyKrNAyKE0eaSqB5r8SzkkrM145zTGH781qiskHXsqFZyrZ6b3DH79AtiqYTv800r9Fb0ALJ');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
 stripe.redirectToCheckout({
   sessionId: sessionId
 })
});